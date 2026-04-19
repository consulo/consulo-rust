/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.toolchain.RsToolchainBase;

/**
 * Utility methods for accessing Rust project settings services.
 */
public final class RsProjectSettingsServiceUtil {
    private RsProjectSettingsServiceUtil() {
    }

    @NotNull
    public static RustProjectSettingsService getRustSettings(@NotNull Project project) {
        return project.getService(RustProjectSettingsService.class);
    }

    @Nullable
    public static RsToolchainBase getToolchain(@NotNull Project project) {
        return getRustSettings(project).getToolchain();
    }

    @NotNull
    public static RsExternalLinterProjectSettingsService getExternalLinterSettings(@NotNull Project project) {
        return project.getService(RsExternalLinterProjectSettingsService.class);
    }

    @NotNull
    public static RustfmtProjectSettingsService getRustfmtSettings(@NotNull Project project) {
        return project.getService(RustfmtProjectSettingsService.class);
    }
}
