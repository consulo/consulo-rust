/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings;

import consulo.project.Project;
import jakarta.annotation.Nonnull;

public final class RustProjectSettingsServiceUtil {
    private RustProjectSettingsServiceUtil() {}

    @Nonnull
    public static RustProjectSettingsService getRustSettings(@Nonnull Project project) {
        return project.getService(RustProjectSettingsService.class);
    }

    /** Delegates to the project-independent {@link RustAdvancedSettings#getMaximumRecursionLimit()}. */
    public static int getMaximumRecursionLimit(@Nonnull Project project) {
        return RustAdvancedSettings.getMaximumRecursionLimit();
    }
}
