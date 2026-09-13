/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace.library;

import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.DependencyScope;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.rust.module.extension.RustModuleExtension;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.api.model.CargoProjectsListener;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.project.workspace.CargoLibraries;
import org.rust.cargo.project.workspace.CargoLibrary;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the project libraries that carry Cargo dependencies in step with the resolved workspace: a
 * package that appears in the workspace gets a library, a package that leaves it loses one, and a
 * package whose roots moved gets its library rewritten so that the platform reindexes it.
 * <p>
 * The libraries are project level and reach a module through an ordinary library order entry, which is
 * what makes the excluded roots of a dependency - its tests, benchmarks and examples - take effect:
 * the root index collects exclusions from library order entries only.
 * <p>
 * A module owning a Cargo project also gets the order entry of its Rust bundle, which is what brings
 * in the standard library sources.
 */
@TopicImpl(ComponentScope.PROJECT)
public class CargoLibrarySynchronizer implements CargoProjectsListener {

    private static final Logger LOG = Logger.getInstance(CargoLibrarySynchronizer.class);

    /**
     * Both the display namespace of the libraries and the mark of the ones this plugin owns: a library
     * whose name does not start with it is never rewritten and never removed.
     */
    private static final String PREFIX = "Cargo: ";

    @Override
    public void cargoProjectsUpdated(@Nonnull CargoProjectsService service, @Nonnull Collection<CargoProject> projects) {
        sync(service.getProject());
    }

    /**
     * Rewrites the Cargo libraries of {@code project} and the module entries pointing at them from the
     * current workspace.
     */
    /**
     * Rewrites the Cargo libraries asynchronously. This is reached from {@code cargoProjectsUpdated},
     * which already runs under a write action, so the work is queued rather than nested: the services
     * it needs are resolved outside any lock, and the model is committed in a write step of its own.
     * Committing those root models is also what tells the platform the roots changed.
     */
    public static void sync(@Nonnull Project project) {
        if (project.isDisposed()) return;

        CoroutineScope.launchAsync(project.coroutineContext(), () -> Coroutine
            .first(CodeExecution.<Void, Services>apply(input -> new Services(
                ProjectLibraryTable.getInstance(project),
                ModuleManager.getInstance(project))))
            .then(WriteLock.<Services, Void>apply(services -> {
                if (project.isDisposed()) return null;
                doSync(project, services.libraryTable(), services.moduleManager());
                return null;
            })));
    }

    /** The services the sync needs, resolved before any lock is taken. */
    private record Services(@Nonnull LibraryTable libraryTable, @Nonnull ModuleManager moduleManager) {
    }

    private static void doSync(@Nonnull Project project,
                               @Nonnull LibraryTable libraryTable,
                               @Nonnull ModuleManager moduleManager) {
        Map<Module, List<CargoLibrary>> librariesByModule = groupByModule(project);

        Map<String, Roots> wanted = new LinkedHashMap<>();
        for (List<CargoLibrary> libraries : librariesByModule.values()) {
            for (CargoLibrary library : libraries) {
                wanted.computeIfAbsent(libraryName(library), name -> new Roots()).add(library);
            }
        }

        LibraryTable.ModifiableModel tableModel = libraryTable.getModifiableModel();

        Map<String, Library> libraries = new LinkedHashMap<>();
        List<Library.ModifiableModel> libraryModels = new ArrayList<>();
        List<ModifiableRootModel> rootModels = new ArrayList<>();
        boolean committing = false;
        try {
            for (Map.Entry<String, Roots> entry : wanted.entrySet()) {
                String name = entry.getKey();
                Library library = tableModel.getLibraryByName(name);
                if (library == null) {
                    library = tableModel.createLibrary(name, CargoLibraryType.KIND);
                }
                libraries.put(name, library);

                Library.ModifiableModel libraryModel = library.getModifiableModel();
                libraryModels.add(libraryModel);
                entry.getValue().applyTo(libraryModel);
            }

            for (Library library : tableModel.getLibraries()) {
                String name = library.getName();
                if (name == null || !name.startsWith(PREFIX) || libraries.containsKey(name)) continue;
                if (library.isDisposed()) continue;
                tableModel.removeLibrary(library);
            }

            for (Module module : moduleManager.getModules()) {
                if (module.isDisposed()) continue;
                List<CargoLibrary> moduleLibraries = librariesByModule.get(module);
                if (moduleLibraries == null && !hasCargoLibraryEntry(module)) continue;

                ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                rootModels.add(rootModel);

                for (OrderEntry orderEntry : rootModel.getOrderEntries()) {
                    if (isCargoLibraryEntry(orderEntry)) {
                        rootModel.removeOrderEntry(orderEntry);
                    }
                }
                if (moduleLibraries != null) {
                    Set<String> attached = new LinkedHashSet<>();
                    for (CargoLibrary library : moduleLibraries) {
                        String name = libraryName(library);
                        if (!attached.add(name)) continue;
                        Library projectLibrary = libraries.get(name);
                        if (projectLibrary == null) continue;
                        LibraryOrderEntry orderEntry = rootModel.addLibraryEntry(projectLibrary);
                        orderEntry.setScope(DependencyScope.COMPILE);
                    }
                    addMissingToolchainEntry(rootModel);
                }
            }

            if (!isChanged(tableModel, libraryModels, rootModels)) {
                return;
            }

            committing = true;
            ProjectRootManager.getInstance(project).mergeRootsChangesDuring(() -> {
                for (Library.ModifiableModel libraryModel : libraryModels) {
                    libraryModel.commit();
                }
                tableModel.commit();
                for (ModifiableRootModel rootModel : rootModels) {
                    rootModel.commit();
                }
            });
        }
        finally {
            if (!committing) {
                for (ModifiableRootModel rootModel : rootModels) {
                    if (!rootModel.isDisposed()) {
                        rootModel.dispose();
                    }
                }
                for (Library.ModifiableModel libraryModel : libraryModels) {
                    Disposer.dispose(libraryModel);
                }
            }
        }
    }

    /**
     * The name a library is found under on the next refresh. It is derived from the coordinates of the
     * package, never from a path that can move, so that a refresh reuses the library rather than
     * replacing it. Two Cargo projects depending on the same crate share one library.
     */
    @Nonnull
    public static String libraryName(@Nonnull CargoLibrary library) {
        if (library.getKind() != CargoLibrary.Kind.GENERATED) {
            return PREFIX + library.getPresentableText();
        }
        // one library of generated code per Cargo project, told apart by the directory of its manifest
        String owner = ownerName(library.getId());
        return owner == null
            ? PREFIX + library.getName()
            : PREFIX + library.getName() + " (" + owner + ")";
    }

    @Nullable
    private static String ownerName(@Nonnull String manifestPath) {
        try {
            Path directory = Path.of(manifestPath).getParent();
            Path name = directory == null ? null : directory.getFileName();
            return name == null ? null : name.toString();
        }
        catch (InvalidPathException e) {
            return null;
        }
    }

    private static boolean isChanged(
        @Nonnull LibraryTable.ModifiableModel tableModel,
        @Nonnull List<Library.ModifiableModel> libraryModels,
        @Nonnull List<ModifiableRootModel> rootModels
    ) {
        if (tableModel.isChanged()) return true;
        for (Library.ModifiableModel libraryModel : libraryModels) {
            if (libraryModel.isChanged()) return true;
        }
        for (ModifiableRootModel rootModel : rootModels) {
            if (rootModel.isChanged()) return true;
        }
        return false;
    }

    @Nonnull
    private static Map<Module, List<CargoLibrary>> groupByModule(@Nonnull Project project) {
        Map<Module, List<CargoLibrary>> result = new LinkedHashMap<>();
        for (CargoProject cargoProject : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            VirtualFile rootDir = cargoProject.getRootDir();
            if (rootDir == null) continue;
            Module module = findModule(project, rootDir);
            if (module == null) {
                LOG.debug("No module owns Cargo project " + cargoProject.getManifest());
                continue;
            }
            result.computeIfAbsent(module, it -> new ArrayList<>()).addAll(CargoLibraries.libraries(cargoProject));
        }
        return result;
    }

    @Nullable
    private static Module findModule(@Nonnull Project project, @Nonnull VirtualFile rootDir) {
        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(rootDir);
        if (module != null) {
            return module;
        }
        for (Module candidate : ModuleManager.getInstance(project).getModules()) {
            for (VirtualFile contentRoot : ModuleRootManager.getInstance(candidate).getContentRoots()) {
                if (VirtualFileUtil.isAncestor(contentRoot, rootDir, false)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Adds the order entry that carries the roots of the bundle the module is bound to, unless the
     * module is not a Rust module, has no bundle selected, or already carries the entry. The standard
     * library reaches the module through it.
     */
    private static void addMissingToolchainEntry(@Nonnull ModifiableRootModel rootModel) {
        RustModuleExtension extension = rootModel.getExtension(RustModuleExtension.class);
        if (extension == null || extension.getInheritableSdk().isNull()) return;
        if (rootModel.findModuleExtensionSdkEntry(extension) != null) return;
        rootModel.addModuleExtensionSdkEntry(extension);
    }

    private static boolean hasCargoLibraryEntry(@Nonnull Module module) {
        for (OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (isCargoLibraryEntry(orderEntry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCargoLibraryEntry(@Nonnull OrderEntry orderEntry) {
        if (!(orderEntry instanceof LibraryOrderEntry)) return false;
        String name = ((LibraryOrderEntry) orderEntry).getLibraryName();
        return name != null && name.startsWith(PREFIX);
    }

    /**
     * What one library is meant to hold, gathered from every {@link CargoLibrary} that resolves to the
     * same name.
     */
    private static final class Roots {

        private final Set<String> mySourceUrls = new LinkedHashSet<>();
        private final Set<String> myExcludedUrls = new LinkedHashSet<>();

        private void add(@Nonnull CargoLibrary library) {
            Set<VirtualFile> sourceRoots = library.getSourceRoots();
            for (VirtualFile sourceRoot : sourceRoots) {
                mySourceUrls.add(sourceRoot.getUrl());
            }
            for (VirtualFile excludedRoot : library.getExcludedRoots()) {
                // an exclusion outside every root of the library is kept by the platform but does nothing
                for (VirtualFile sourceRoot : sourceRoots) {
                    if (VirtualFileUtil.isAncestor(sourceRoot, excludedRoot, true)) {
                        myExcludedUrls.add(excludedRoot.getUrl());
                        break;
                    }
                }
            }
        }

        private void applyTo(@Nonnull Library.ModifiableModel model) {
            // the same directories answer both root types: the source root is what the indexer walks,
            // while the search scope a module builds out of its order entries is assembled from the
            // binaries roots of everything that is not a module, so a crate published under sources
            // alone would resolve to nothing
            updateRoots(model, SourcesOrderRootType.ID);
            updateRoots(model, BinariesOrderRootType.ID);
            // after the roots, because removing a root drops the exclusions that fall outside what is left
            updateExcludedRoots(model);
        }

        private void updateRoots(@Nonnull Library.ModifiableModel model, @Nonnull String rootType) {
            Set<String> present = new LinkedHashSet<>(Arrays.asList(model.getUrls(rootType)));
            for (String url : present) {
                if (!mySourceUrls.contains(url)) {
                    model.removeRoot(url, rootType);
                }
            }
            for (String url : mySourceUrls) {
                if (!present.contains(url)) {
                    model.addRoot(url, rootType);
                }
            }
        }

        private void updateExcludedRoots(@Nonnull Library.ModifiableModel model) {
            Set<String> present = new LinkedHashSet<>(Arrays.asList(model.getExcludedRootUrls()));
            for (String url : present) {
                if (!myExcludedUrls.contains(url)) {
                    model.removeExcludedRoot(url);
                }
            }
            for (String url : myExcludedUrls) {
                if (!present.contains(url)) {
                    model.addExcludedRoot(url);
                }
            }
        }
    }
}
