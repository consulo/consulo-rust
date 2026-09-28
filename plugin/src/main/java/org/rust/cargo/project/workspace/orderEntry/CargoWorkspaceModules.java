/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace.orderEntry;

import consulo.logging.Logger;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.project.Project;
import consulo.rust.module.extension.RustModuleExtension;
import consulo.rust.module.extension.RustMutableModuleExtension;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gives every workspace member package a module of its own, so that the project model names the member
 * a file belongs to and {@code ProjectFileIndex} can answer for it.
 * <p>
 * The member whose directory is already the content root of the module that owns the manifest keeps
 * that module - two modules may not share a content root - and only the members below it get modules
 * added. Nesting is the ordinary Maven shape: the root index resolves a file to the nearest content
 * root, so a member below the workspace root wins over the workspace root itself.
 */
public final class CargoWorkspaceModules {

    private static final Logger LOG = Logger.getInstance(CargoWorkspaceModules.class);

    private CargoWorkspaceModules() {
    }

    /** One workspace member together with the Cargo project it came from. */
    public record Member(@Nonnull CargoWorkspace.Package pkg, @Nonnull VirtualFile contentRoot, @Nonnull String manifestPath) {
    }

    /**
     * Creates a module for every member that has none and disposes the modules of members that have
     * left the workspace.
     *
     * @return the module of each member, by package id, including members that reuse an existing module
     */
    @Nonnull
    public static Map<String, Module> reconcile(
        @Nonnull Project project,
        @Nonnull ModuleManager moduleManager,
        @Nonnull Map<Module, List<Member>> membersByOwner
    ) {
        Map<String, Module> byPackageId = new LinkedHashMap<>();
        Set<String> wanted = new LinkedHashSet<>();
        for (List<Member> members : membersByOwner.values()) {
            for (Member member : members) {
                wanted.add(member.pkg().getId());
            }
        }

        List<Module> stale = staleModules(moduleManager, wanted);
        List<Member> missing = new ArrayList<>();

        for (Map.Entry<Module, List<Member>> entry : membersByOwner.entrySet()) {
            Module owner = entry.getKey();
            for (Member member : entry.getValue()) {
                Module existing = findModuleFor(moduleManager, member);
                if (existing != null) {
                    byPackageId.put(member.pkg().getId(), existing);
                }
                else {
                    missing.add(member);
                }
            }
            // the owner keeps its own content root, whichever member sits on it
            LOG.debug("Cargo project of module " + owner.getName() + " has " + entry.getValue().size() + " member(s)");
        }

        if (missing.isEmpty() && stale.isEmpty()) {
            return byPackageId;
        }

        ModifiableModuleModel moduleModel = moduleManager.getModifiableModel();
        boolean committed = false;
        try {
            for (Module module : stale) {
                LOG.info("Disposing module of a Cargo workspace member that left the workspace: " + module.getName());
                moduleModel.disposeModule(module);
            }
            for (Member member : missing) {
                String name = uniqueName(moduleModel, member.pkg().getName());
                Module module = moduleModel.newModule(name, member.contentRoot().getPath());
                byPackageId.put(member.pkg().getId(), module);
            }
            moduleModel.commit();
            committed = true;
        }
        finally {
            if (!committed) {
                moduleModel.dispose();
            }
        }
        return byPackageId;
    }

    /**
     * Brings the content root, source folders, toolchain and owning-module dependency of one member
     * module up to date. The caller owns the model and commits it.
     */
    public static void configure(
        @Nonnull ModifiableRootModel rootModel,
        @Nonnull Member member,
        @Nonnull Module owner
    ) {
        VirtualFile contentRoot = member.contentRoot();

        boolean hasContentRoot = false;
        for (ContentEntry contentEntry : rootModel.getContentEntries()) {
            if (contentRoot.equals(contentEntry.getFile())) {
                hasContentRoot = true;
                CargoProjectServiceUtil.setup(contentEntry, contentRoot);
            }
        }
        if (!hasContentRoot) {
            CargoProjectServiceUtil.setup(rootModel.addContentEntry(contentRoot), contentRoot);
        }

        RustMutableModuleExtension extension = rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
        if (extension != null) {
            extension.setEnabled(true);
            extension.setCargoPackageId(member.pkg().getId());
            extension.setCargoManifestPath(member.manifestPath());
            if (!owner.equals(rootModel.getModule())) {
                // inherited rather than bound, so switching the toolchain on the owner moves every member
                extension.getInheritableSdk().set(owner, null);
            }
        }

        if (!owner.equals(rootModel.getModule()) && !dependsOn(rootModel, owner)) {
            // the owner carries the dependency entries, exported, so they reach the member through this
            rootModel.addModuleOrderEntry(owner);
        }
    }

    /**
     * Members of {@code cargoProject} that have a content root, keyed by nothing - the caller groups
     * them by the module that owns the manifest.
     */
    @Nonnull
    public static List<Member> membersOf(@Nonnull CargoProject cargoProject) {
        CargoWorkspace workspace = cargoProject.getWorkspace();
        if (workspace == null) {
            return List.of();
        }
        String manifestPath = cargoProject.getManifest().toString();
        List<Member> result = new ArrayList<>();
        for (CargoWorkspace.Package pkg : workspace.getPackages()) {
            if (pkg.getOrigin() != PackageOrigin.WORKSPACE) continue;
            VirtualFile contentRoot = pkg.getContentRoot();
            if (contentRoot == null) continue;
            result.add(new Member(pkg, contentRoot, manifestPath));
        }
        return result;
    }

    @Nullable
    private static Module findModuleFor(@Nonnull ModuleManager moduleManager, @Nonnull Member member) {
        for (Module module : moduleManager.getModules()) {
            if (module.isDisposed()) continue;
            RustModuleExtension extension = RustModuleExtension.findExtension(module);
            if (extension != null && member.pkg().getId().equals(extension.getCargoPackageId())) {
                return module;
            }
        }
        // a member whose directory is already a module content root keeps that module, which is how the
        // module the user imported stays the module of the package sitting at the project root
        for (Module module : moduleManager.getModules()) {
            if (module.isDisposed()) continue;
            for (VirtualFile contentRoot : ModuleRootManager.getInstance(module).getContentRoots()) {
                if (member.contentRoot().equals(contentRoot)) {
                    return module;
                }
            }
        }
        return null;
    }

    /**
     * Modules this plugin created for a member that is no longer in any workspace. A module without the
     * mark is never touched, and neither is one that owns a manifest of its own.
     */
    @Nonnull
    private static List<Module> staleModules(@Nonnull ModuleManager moduleManager, @Nonnull Set<String> wantedPackageIds) {
        List<Module> result = new ArrayList<>();
        for (Module module : moduleManager.getModules()) {
            if (module.isDisposed()) continue;
            RustModuleExtension extension = RustModuleExtension.findExtension(module);
            if (extension == null) continue;
            String packageId = extension.getCargoPackageId();
            if (packageId == null || wantedPackageIds.contains(packageId)) continue;
            if (ownsManifest(module)) continue;
            result.add(module);
        }
        return result;
    }

    private static boolean ownsManifest(@Nonnull Module module) {
        for (VirtualFile contentRoot : ModuleRootManager.getInstance(module).getContentRoots()) {
            if (contentRoot.findChild(org.rust.cargo.CargoConstants.MANIFEST_FILE) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean dependsOn(@Nonnull ModifiableRootModel rootModel, @Nonnull Module owner) {
        for (consulo.module.content.layer.orderEntry.OrderEntry orderEntry : rootModel.getOrderEntries()) {
            if (orderEntry instanceof consulo.module.content.layer.orderEntry.ModuleOrderEntry moduleOrderEntry
                && owner.equals(moduleOrderEntry.getModule())) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private static String uniqueName(@Nonnull ModifiableModuleModel moduleModel, @Nonnull String preferred) {
        if (moduleModel.findModuleByName(preferred) == null) {
            return preferred;
        }
        for (int i = 2; i < 1000; i++) {
            String candidate = preferred + "~" + i;
            if (moduleModel.findModuleByName(candidate) == null) {
                return candidate;
            }
        }
        return preferred + "~" + System.nanoTime();
    }
}
