/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Delegates to MacroExpansionManagerUtil.
 */
public final class RsMacroExpansionManagerUtil {
    private RsMacroExpansionManagerUtil() {
    }

    @NotNull
    public static MacroExpansionManager getMacroExpansionManager(@NotNull Project project) {
        return MacroExpansionManagerUtil.getMacroExpansionManager(project);
    }
}
