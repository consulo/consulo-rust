/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace.orderEntry;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.concurrent.coroutine.WriteLock;
import consulo.content.library.Library;
import consulo.content.library.LibraryTable;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.CustomOrderEntry;
import consulo.module.content.layer.orderEntry.CustomOrderEntryModel;
import consulo.module.content.layer.orderEntry.ExportableOrderEntry;
import consulo.module.content.layer.orderEntry.LibraryOrderEntry;
import consulo.module.content.layer.orderEntry.ModuleExtensionWithSdkOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.project.content.library.ProjectLibraryTable;
import consulo.rust.module.extension.RustModuleExtension;
import consulo.rust.module.extension.RustMutableModuleExtension;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsListener;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.CargoLibraries;
import org.rust.cargo.project.workspace.CargoLibrary;
import org.rust.cargo.project.workspace.CargoWorkspaceFactory;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.CargoWorkspaceData;
import org.rust.cargo.project.workspace.state.CargoWorkspaceStates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps the Cargo order entries of every module in step with the resolved workspace: a package that
 * appears in the workspace gets an entry, a package that leaves it loses one, and a package whose roots
 * moved gets its entry rewritten so that the platform reindexes it.
 * <p>
 * The entries are {@link CustomOrderEntry custom order entries} rather than project libraries, so that
 * a package is a thing the project model names in its own right: the roots and the package coordinates
 * are written into the module file, and {@code ProjectFileIndex} can answer which package a file
 * belongs to without the plugin keeping an index of its own.
 * <p>
 * A module owning a Cargo project also gets the order entry of its Rust bundle, which is what binds it
 * to a toolchain.
 */
@TopicImpl(ComponentScope.PROJECT)
public class CargoLibraryRootsSynchronizer implements CargoProjectsListener {

    private static final Logger LOG = Logger.getInstance(CargoLibraryRootsSynchronizer.class);

    /** Name prefix of the project libraries this plugin used to create, removed on the first sync. */
    private static final String LEGACY_LIBRARY_PREFIX = "Cargo: ";

    @Override
    public void cargoProjectsUpdated(@Nonnull CargoProjectsService service, @Nonnull Collection<CargoProject> projects) {
        sync(service.getProject());
    }

    /**
     * Rewrites the Cargo order entries of {@code project} from the current workspace.
     * <p>
     * This is reached from {@code cargoProjectsUpdated}, which already runs under a write action, so the
     * work is queued rather than nested: the services it needs are resolved outside any lock, and the
     * model is committed in a write step of its own. Committing those root models is also what tells the
     * platform the roots changed.
     */
    public static void sync(@Nonnull Project project) {
        if (project.isDisposed()) return;

        CoroutineScope.launchAsync(project.coroutineContext(), () -> Coroutine
            .first(CodeExecution.<Void, Services>apply(input -> new Services(
                ProjectLibraryTable.getInstance(project),
                ModuleManager.getInstance(project),
                CargoLibraryOrderEntryType.getInstance())))
            .then(WriteLock.<Services, Void>apply(services -> {
                if (project.isDisposed()) return null;
                doSync(project, services);
                return null;
            })));
    }

    /** The services the sync needs, resolved before any lock is taken. */
    private record Services(
        @Nonnull LibraryTable libraryTable,
        @Nonnull ModuleManager moduleManager,
        @Nonnull CargoLibraryOrderEntryType entryType
    ) {
    }

    /** One library together with the Cargo project that contributed it. */
    private record OwnedLibrary(@Nonnull CargoLibrary library, @Nonnull String manifestPath) {
    }

    private static void doSync(@Nonnull Project project, @Nonnull Services services) {
        Map<Module, List<OwnedLibrary>> librariesByModule = groupByModule(project, services.moduleManager());
        List<Library> legacyLibraries = legacyLibraries(services.libraryTable());

        // members first: a member may need a module of its own before anything can be hung on it
        Map<Module, CargoProject> ownedProjects = groupProjectsByModule(project, services.moduleManager());
        Map<Module, List<CargoWorkspaceModules.Member>> membersByOwner = groupMembersByModule(project, services.moduleManager());
        Map<String, Module> memberModules =
            CargoWorkspaceModules.reconcile(project, services.moduleManager(), membersByOwner);
        Map<Module, CargoWorkspaceModules.Member> memberOf = new LinkedHashMap<>();
        Map<Module, Module> ownerOf = new LinkedHashMap<>();
        for (Map.Entry<Module, List<CargoWorkspaceModules.Member>> entry : membersByOwner.entrySet()) {
            for (CargoWorkspaceModules.Member member : entry.getValue()) {
                Module module = memberModules.get(member.pkg().getId());
                if (module == null || module.isDisposed()) continue;
                memberOf.put(module, member);
                ownerOf.put(module, entry.getKey());
            }
        }

        List<ModifiableRootModel> rootModels = new ArrayList<>();
        boolean committing = false;
        try {
            for (Module module : services.moduleManager().getModules()) {
                if (module.isDisposed()) continue;
                List<OwnedLibrary> moduleLibraries = librariesByModule.get(module);
                CargoWorkspaceModules.Member member = memberOf.get(module);
                if (moduleLibraries == null && member == null && !ownedProjects.containsKey(module)
                    && !hasEntriesToDrop(module) && !needsToolchainEntry(module)) continue;

                ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
                rootModels.add(rootModel);

                for (OrderEntry orderEntry : rootModel.getOrderEntries()) {
                    if (isCargoEntry(orderEntry) || isLegacyCargoLibraryEntry(orderEntry)) {
                        rootModel.removeOrderEntry(orderEntry);
                    }
                }
                if (moduleLibraries != null) {
                    for (OwnedLibrary owned : moduleLibraries) {
                        CustomOrderEntry<CargoLibraryOrderEntryModel> entry = rootModel.addCustomOderEntry(
                            services.entryType(),
                            new CargoLibraryOrderEntryModel(owned.library(), owned.manifestPath())
                        );
                        // exported, so a module that depends on this one - a workspace member on the module
                        // owning the manifest - sees the dependency roots without carrying a copy of every entry
                        if (entry instanceof ExportableOrderEntry exportable) {
                            exportable.setExported(true);
                        }
                    }
                }
                if (member != null) {
                    CargoWorkspaceModules.configure(rootModel, member, ownerOf.get(module));
                }
                CargoProject ownedProject = ownedProjects.get(module);
                if (ownedProject != null) {
                    storeWorkspace(rootModel, ownedProject);
                }
                addMissingToolchainEntry(rootModel);
            }

            LibraryTable.ModifiableModel tableModel = legacyLibraries.isEmpty() ? null : services.libraryTable().getModifiableModel();
            if (tableModel != null) {
                for (Library library : legacyLibraries) {
                    tableModel.removeLibrary(library);
                }
            }

            if (!isChanged(tableModel, rootModels)) {
                LOG.info("Cargo order entries already up to date");
                return;
            }

            int entryCount = 0;
            for (List<OwnedLibrary> owned : librariesByModule.values()) {
                entryCount += owned.size();
            }
            LOG.info("Writing " + entryCount + " Cargo order entries across " + librariesByModule.size()
                + " module(s), " + memberOf.size() + " workspace member module(s), dropping "
                + legacyLibraries.size() + " legacy project library(ies)");

            committing = true;
            ProjectRootManager.getInstance(project).mergeRootsChangesDuring(() -> {
                if (tableModel != null) {
                    tableModel.commit();
                }
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
            }
        }
    }

    private static boolean isChanged(@Nullable LibraryTable.ModifiableModel tableModel, @Nonnull List<ModifiableRootModel> rootModels) {
        if (tableModel != null && tableModel.isChanged()) return true;
        for (ModifiableRootModel rootModel : rootModels) {
            if (rootModel.isChanged()) return true;
        }
        return false;
    }

    @Nonnull
    private static Map<Module, List<OwnedLibrary>> groupByModule(@Nonnull Project project, @Nonnull ModuleManager moduleManager) {
        Map<Module, List<OwnedLibrary>> result = new LinkedHashMap<>();
        for (CargoProject cargoProject : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            VirtualFile rootDir = cargoProject.getRootDir();
            if (rootDir == null) continue;
            Module module = findModule(project, moduleManager, rootDir);
            if (module == null) {
                LOG.debug("No module owns Cargo project " + cargoProject.getManifest());
                continue;
            }
            String manifestPath = cargoProject.getManifest().toString();
            List<OwnedLibrary> owned = result.computeIfAbsent(module, it -> new ArrayList<>());
            for (CargoLibrary library : CargoLibraries.libraries(cargoProject)) {
                owned.add(new OwnedLibrary(library, manifestPath));
            }
        }
        return result;
    }

    /** The workspace members of every attached Cargo project, keyed by the module owning its manifest. */
    @Nonnull
    private static Map<Module, List<CargoWorkspaceModules.Member>> groupMembersByModule(
        @Nonnull Project project,
        @Nonnull ModuleManager moduleManager
    ) {
        Map<Module, List<CargoWorkspaceModules.Member>> result = new LinkedHashMap<>();
        for (CargoProject cargoProject : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            VirtualFile rootDir = cargoProject.getRootDir();
            if (rootDir == null) continue;
            Module owner = findModule(project, moduleManager, rootDir);
            if (owner == null) continue;
            List<CargoWorkspaceModules.Member> members = CargoWorkspaceModules.membersOf(cargoProject);
            if (!members.isEmpty()) {
                result.computeIfAbsent(owner, it -> new ArrayList<>()).addAll(members);
            }
        }
        return result;
    }

    /** The Cargo project each owning module carries, so its resolved workspace can be written onto it. */
    @Nonnull
    private static Map<Module, CargoProject> groupProjectsByModule(
        @Nonnull Project project,
        @Nonnull ModuleManager moduleManager
    ) {
        Map<Module, CargoProject> result = new LinkedHashMap<>();
        for (CargoProject cargoProject : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            VirtualFile rootDir = cargoProject.getRootDir();
            if (rootDir == null) continue;
            Module owner = findModule(project, moduleManager, rootDir);
            if (owner != null) {
                result.putIfAbsent(owner, cargoProject);
            }
        }
        return result;
    }

    /**
     * Writes the resolved workspace onto the module that owns the manifest, which is what lets the next
     * open rebuild it without running Cargo.
     */
    private static void storeWorkspace(@Nonnull ModifiableRootModel rootModel, @Nonnull CargoProject cargoProject) {
        RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
        if (extension == null) return;
        CargoWorkspace workspace = cargoProject.getWorkspace();
        CargoWorkspaceData data = CargoWorkspaceFactory.dataOf(workspace);
        extension.setCargoWorkspaceState(
            data == null ? null : CargoWorkspaceStates.toState(data, CargoWorkspaceFactory.cfgOptionsOf(workspace)));
    }

    @Nullable
    private static Module findModule(@Nonnull Project project, @Nonnull ModuleManager moduleManager, @Nonnull VirtualFile rootDir) {
        Module module = ProjectFileIndex.getInstance(project).getModuleForFile(rootDir);
        if (module != null) {
            return module;
        }
        for (Module candidate : moduleManager.getModules()) {
            for (VirtualFile contentRoot : ModuleRootManager.getInstance(candidate).getContentRoots()) {
                if (VirtualFileUtil.isAncestor(contentRoot, rootDir, false)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /**
     * Adds the order entry that binds the module to its Rust bundle, unless the module is not a Rust
     * module, has no bundle selected, or already carries the entry.
     */
    private static void addMissingToolchainEntry(@Nonnull ModifiableRootModel rootModel) {
        RustModuleExtension extension = rootModel.getExtension(RustModuleExtension.class);
        if (extension == null || extension.getInheritableSdk().isNull()) return;
        if (rootModel.findModuleExtensionSdkEntry(extension) != null) return;
        rootModel.addModuleExtensionSdkEntry(extension);
    }

    /** Answered from the committed model, so a module that needs nothing costs no modifiable model. */
    private static boolean needsToolchainEntry(@Nonnull Module module) {
        RustModuleExtension extension = RustModuleExtension.findExtension(module);
        if (extension == null || !extension.isEnabled() || extension.getInheritableSdk().isNull()) return false;
        for (OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (orderEntry instanceof ModuleExtensionWithSdkOrderEntry) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasEntriesToDrop(@Nonnull Module module) {
        for (OrderEntry orderEntry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (isCargoEntry(orderEntry) || isLegacyCargoLibraryEntry(orderEntry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCargoEntry(@Nonnull OrderEntry orderEntry) {
        if (!(orderEntry instanceof CustomOrderEntry<?> customOrderEntry)) return false;
        CustomOrderEntryModel model = customOrderEntry.getModel();
        return model instanceof CargoLibraryOrderEntryModel;
    }

    /** A library order entry left behind by the version of this plugin that used project libraries. */
    private static boolean isLegacyCargoLibraryEntry(@Nonnull OrderEntry orderEntry) {
        if (!(orderEntry instanceof LibraryOrderEntry libraryOrderEntry)) return false;
        String name = libraryOrderEntry.getLibraryName();
        return name != null && name.startsWith(LEGACY_LIBRARY_PREFIX);
    }

    @Nonnull
    private static List<Library> legacyLibraries(@Nonnull LibraryTable libraryTable) {
        List<Library> result = new ArrayList<>();
        for (Library library : libraryTable.getLibraries()) {
            String name = library.getName();
            if (name != null && name.startsWith(LEGACY_LIBRARY_PREFIX) && !library.isDisposed()) {
                result.add(library);
            }
        }
        return result;
    }
}
