/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import java.util.concurrent.Future;
import consulo.util.concurrent.coroutine.step.CodeExecution;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.application.concurrent.coroutine.WriteLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import consulo.rust.module.extension.RustModuleExtension;
import consulo.util.dataholder.Key;
import consulo.component.PropertiesComponent;
import consulo.project.ui.notification.NotificationType;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.module.content.layer.ContentEntry;
import consulo.application.util.registry.Registry;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.settings.RustProjectSettingsService;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.ide.notifications.NotificationUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import consulo.project.ProjectPropertiesComponent;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import consulo.logging.Logger;
import java.util.ArrayList;
import java.util.List;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkTable;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.rust.bundle.RustBundleType;
import consulo.rust.module.extension.RustMutableModuleExtension;

public final class CargoProjectServiceUtil {
    private static final Logger LOG = Logger.getInstance(CargoProjectServiceUtil.class);


    /** The setup currently running for a project, so notification passes do not queue more of them. */
    private static final Key<Future<?>> SETUP_IN_FLIGHT = Key.create("org.rust.cargo.setupInFlight");

    /** Refreshes attempted for a workspace that never materialised. */
    private static final Key<AtomicInteger> REFRESH_ATTEMPTS = Key.create("org.rust.cargo.refreshAttempts");

    private static final int MAX_REFRESH_ATTEMPTS = 3;

    /** When the running setup started, so a stalled one can be superseded. */
    private static final Key<Long> SETUP_STARTED_AT = Key.create("org.rust.cargo.setupStartedAt");

    private static final long SETUP_GUARD_TIMEOUT_NANOS = 60L * 1_000_000_000L;

    private CargoProjectServiceUtil() {
    }

    @Nonnull
    public static CargoProjectsService getCargoProjects(@Nonnull Project project) {
        return project.getService(CargoProjectsService.class);
    }

    public static boolean isGeneratedFile(@Nonnull CargoProjectsService service, @Nonnull VirtualFile file) {
        CargoWorkspace.Package pkg = service.findPackageForFile(file);
        if (pkg == null) return false;
        VirtualFile outDir = pkg.getOutDir();
        if (outDir == null) return false;
        return VirtualFileUtil.isAncestor(outDir, file, false);
    }

    public static boolean guessAndSetupRustProject(@Nonnull Project project, boolean explicitRequest) {
        boolean discover = explicitRequest;
        if (!explicitRequest) {
            String key = "org.rust.cargo.project.model.PROJECT_DISCOVERY";
            PropertiesComponent properties = ProjectPropertiesComponent.getInstance(project);
            discover = !properties.getBoolean(key);
            properties.setValue(key, true);
        }

        CargoProjectsService cargoProjects = getCargoProjects(project);
        boolean hasWorkspace = cargoProjects.getHasAtLeastOneValidProject();

        // Editor notifications ask for this on every pass over every file, so the common case - nothing
        // left to do - has to be answered without touching the module model. Building one takes the
        // write lock, which restarts the very analysis that asked.
        boolean needsBinding = anyModuleNeedsBinding(project);
        // A project restored from disk reports itself valid while carrying only its manifest path - the
        // packages arrive with the first refresh. An explicit request therefore always refreshes, and
        // only the notification passes lean on the valid flag.
        boolean needsRefresh = !cargoProjects.getAllProjects().isEmpty()
            ? (explicitRequest || !hasWorkspace)
            : (!hasWorkspace && discover);
        if (!needsBinding && !needsRefresh) {
            return discover;
        }

        if (needsRefresh && refreshAttempts(project).incrementAndGet() > MAX_REFRESH_ATTEMPTS) {
            // The workspace cannot be produced - a broken manifest, a toolchain that cannot run. Retrying
            // on every notification pass would spin forever, so let the banner report it instead.
            return discover;
        }

        synchronized (SETUP_IN_FLIGHT) {
            Future<?> running = project.getUserData(SETUP_IN_FLIGHT);
            Long startedAt = project.getUserData(SETUP_STARTED_AT);
            boolean stillRunning = running != null && !running.isDone();
            // A setup that never completes - a coroutine that could not start, a refresh that died -
            // must not block every later attempt, so the guard expires.
            boolean expired = startedAt != null
                && System.nanoTime() - startedAt > SETUP_GUARD_TIMEOUT_NANOS;
            if (stillRunning && !expired) {
                return discover;
            }
            project.putUserData(SETUP_STARTED_AT, System.nanoTime());
            project.putUserData(SETUP_IN_FLIGHT, setupAsync(project, cargoProjects, discover));
        }
        return discover;
    }

    /**
     * Binds the modules of {@code project} to a toolchain bundle and brings its cargo projects up to
     * date. The module model is written under the write lock; the refresh that follows resolves
     * services of its own and so runs outside it.
     *
     * @return a future completing once the refresh it started has finished
     */
    @Nonnull
    private static Future<?> setupAsync(
        @Nonnull Project project,
        @Nonnull CargoProjectsService cargoProjects,
        boolean discover
    ) {
        CompletableFuture<Void> refreshed = new CompletableFuture<>();

        Coroutine<Void, Void> chain = Coroutine
            .first(WriteLock.<Void, Void>apply(input -> {
                setupRustModules(project);
                return null;
            }))
            .then(CodeExecution.<Void, Void>apply(input -> {
                CompletableFuture<?> started = null;
                try {
                    if (!cargoProjects.getAllProjects().isEmpty()) {
                        started = cargoProjects.refreshAllProjects();
                    }
                    else if (discover) {
                        started = cargoProjects.discoverAndRefresh();
                    }
                }
                finally {
                    if (started == null) {
                        refreshed.complete(null);
                    }
                    else {
                        started.whenComplete((ignored, error) -> refreshed.complete(null));
                    }
                }
                return null;
            }));

        try {
            CoroutineScope.launchAsync(project.coroutineContext(), () -> chain);
        }
        catch (Throwable e) {
            // The guard is the future, so a chain that never starts must not leave it pending - that
            // would block every later attempt for the lifetime of the project.
            LOG.warn("Failed to start Rust project setup", e);
            refreshed.complete(null);
        }
        return refreshed;
    }

    /** True when some module owns a manifest but is not bound to a toolchain bundle yet. */
    private static boolean anyModuleNeedsBinding(@Nonnull Project project) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (!isAlreadyBound(module) && ownsManifest(module)) {
                return true;
            }
        }
        return false;
    }


    @Nonnull
    private static AtomicInteger refreshAttempts(@Nonnull Project project) {
        AtomicInteger existing = project.getUserData(REFRESH_ATTEMPTS);
        if (existing != null) return existing;
        AtomicInteger created = new AtomicInteger();
        project.putUserData(REFRESH_ATTEMPTS, created);
        return created;
    }

    public static boolean guessAndSetupRustProject(@Nonnull Project project) {
        return guessAndSetupRustProject(project, false);
    }

    /**
     * Turns every module whose content holds a Cargo manifest into a Rust module bound to a toolchain
     * bundle. The toolchain of a project is read back from that binding, so a module which owns a
     * Cargo project but carries no Rust extension would otherwise have no toolchain at all.
     */
    private static void setupRustModules(@Nonnull Project project) {
        Sdk toolchainBundle = SdkTable.getInstance()
            .findMostRecentSdk(sdk -> sdk.getSdkType() instanceof RustBundleType);
        if (toolchainBundle == null) {
            return;
        }

        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (isAlreadyBound(module) || !ownsManifest(module)) {
                continue;
            }
            ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
            RustMutableModuleExtension extension =
                rootModel.getExtensionWithoutCheck(RustMutableModuleExtension.class);
            if (extension == null || (extension.isEnabled() && !extension.getInheritableSdk().isNull())) {
                rootModel.dispose();
                continue;
            }

            extension.setEnabled(true);
            extension.getInheritableSdk().set(null, toolchainBundle);
            if (rootModel.findModuleExtensionSdkEntry(extension) == null) {
                rootModel.addModuleExtensionSdkEntry(extension);
            }

            if (rootModel.isChanged()) {
                rootModel.commit();
            }
            else {
                rootModel.dispose();
            }
        }
    }

    /** Answered from the committed model, so a module that needs nothing costs no modifiable model. */
    private static boolean isAlreadyBound(@Nonnull Module module) {
        RustModuleExtension extension = RustModuleExtension.findExtension(module);
        return extension != null && extension.isEnabled() && extension.getSdk() != null;
    }

    private static boolean ownsManifest(@Nonnull Module module) {
        for (VirtualFile contentRoot : ModuleRootManager.getInstance(module).getContentRoots()) {
            if (contentRoot.findChild(CargoConstants.MANIFEST_FILE) != null) {
                return true;
            }
        }
        return false;
    }

    public static void setup(@Nonnull ContentEntry contentEntry, @Nonnull VirtualFile contentRoot) {
        setup(new ContentEntryWrapper(contentEntry), contentRoot);
    }

    public static void setup(@Nonnull ContentEntryWrapper wrapper, @Nonnull VirtualFile contentRoot) {
        for (String dirName : CargoConstants.ProjectLayout.sources) {
            VirtualFile child = contentRoot.findChild(dirName);
            if (child != null) {
                wrapper.addSourceFolder(child.getUrl(), false);
            }
        }
        for (String dirName : CargoConstants.ProjectLayout.tests) {
            VirtualFile child = contentRoot.findChild(dirName);
            if (child != null) {
                wrapper.addSourceFolder(child.getUrl(), true);
            }
        }
        VirtualFile targetChild = contentRoot.findChild(CargoConstants.ProjectLayout.target);
        if (targetChild != null) {
            wrapper.addExcludeFolder(targetChild.getUrl());
        }
    }

    public static boolean isNewProjectModelImportEnabled() {
        return Registry.is("org.rust.cargo.new.auto.import", false);
    }
}
