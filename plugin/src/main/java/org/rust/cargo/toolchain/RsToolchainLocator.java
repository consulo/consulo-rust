package org.rust.cargo.toolchain;

import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import consulo.rust.module.extension.RustModuleExtension;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.settings.RustProjectSettings;

/**
 * Finds the toolchain a project is configured with.
 */
public final class RsToolchainLocator {
    private RsToolchainLocator() {
    }

    /**
     * The toolchain of {@code project}, taken from the Rust bundle of its first Rust module. A project
     * whose modules carry no Rust extension has no toolchain.
     */
    @Nullable
    public static RsToolchainBase getToolchain(@Nonnull Project project) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            RsToolchainBase toolchain = RustModuleExtension.findToolchain(module);
            if (toolchain != null) {
                return toolchain;
            }
        }

        return null;
    }
    /**
     * The toolchain the project is configured with, built from the persisted home directory.
     */
    @Nullable
    public static RsToolchainBase fromSettings(@Nonnull Project project) {
        RustProjectSettings state = RsProjectSettingsServiceUtil.getRustSettings(project).getState();
        String home = state.toolchainHomeDirectory;
        // the home is a path of the machine the project lives on
        return home != null ? RsToolchainProvider.getToolchainStatic(project.getPlatform().fs().getPath(home), project.getPlatform()) : null;
    }

    /** The toolchain described by {@code state}; the inverse of {@link #store}. */
    @Nullable
    public static RsToolchainBase load(@Nonnull RustProjectSettings state) {
        String home = state.toolchainHomeDirectory;
        return home != null ? RsToolchainProvider.getToolchainStatic(java.nio.file.Paths.get(home)) : null;
    }

    /** Persists {@code toolchain} as the project's toolchain. */
    public static void store(@Nonnull RustProjectSettings state,
                             @Nullable RsToolchainBase toolchain) {
        state.toolchainHomeDirectory = toolchain != null
            ? toolchain.getLocation().toString().replace('\\', '/')
            : null;
    }
}
