/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class RustProjectSettingsServiceUtil {
    private RustProjectSettingsServiceUtil() {}

    @NotNull
    public static RustProjectSettingsService getRustSettings(@NotNull Project project) {
        return project.getService(RustProjectSettingsService.class);
    }

    /** Delegates to the project-independent {@link RustAdvancedSettings#getMaximumRecursionLimit()}. */
    public static int getMaximumRecursionLimit(@NotNull Project project) {
        return RustAdvancedSettings.getMaximumRecursionLimit();
    }
}
