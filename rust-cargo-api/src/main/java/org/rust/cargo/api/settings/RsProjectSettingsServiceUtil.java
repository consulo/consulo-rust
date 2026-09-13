/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.settings;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.module.Module;
import consulo.module.ModuleManager;

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


    @Nonnull
    public static RsExternalLinterProjectSettingsService getExternalLinterSettings(@Nonnull Project project) {
        return project.getService(RsExternalLinterProjectSettingsService.class);
    }

    @Nonnull
    public static RustfmtProjectSettingsService getRustfmtSettings(@Nonnull Project project) {
        return project.getService(RustfmtProjectSettingsService.class);
    }
}
