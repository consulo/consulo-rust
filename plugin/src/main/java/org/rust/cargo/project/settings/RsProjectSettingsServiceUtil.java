/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.toolchain.RsToolchainBase;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.rust.module.extension.RustModuleExtension;

/**
 * Utility methods for accessing Rust project settings services.
 */
public final class RsProjectSettingsServiceUtil {
    private RsProjectSettingsServiceUtil() {
    }

    @Nonnull
    public static RustProjectSettingsService getRustSettings(@Nonnull Project project) {
        return project.getService(RustProjectSettingsService.class);
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

    @Nonnull
    public static RsExternalLinterProjectSettingsService getExternalLinterSettings(@Nonnull Project project) {
        return project.getService(RsExternalLinterProjectSettingsService.class);
    }

    @Nonnull
    public static RustfmtProjectSettingsService getRustfmtSettings(@Nonnull Project project) {
        return project.getService(RustfmtProjectSettingsService.class);
    }
}
