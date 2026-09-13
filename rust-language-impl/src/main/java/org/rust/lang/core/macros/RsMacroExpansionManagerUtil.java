/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.project.Project;
import jakarta.annotation.Nonnull;

/**
 * Delegates to MacroExpansionManagerUtil.
 */
public final class RsMacroExpansionManagerUtil {
    private RsMacroExpansionManagerUtil() {
    }

    @Nonnull
    public static MacroExpansionManager getMacroExpansionManager(@Nonnull Project project) {
        return MacroExpansionManagerUtil.getMacroExpansionManager(project);
    }
}
