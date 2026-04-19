/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility methods for MacroExpansionManager.
 */
public final class MacroExpansionManagerUtil {
    private MacroExpansionManagerUtil() {
    }

    public static final Logger MACRO_LOG = Logger.getInstance(MacroExpansionManager.class);

    @NotNull
    public static MacroExpansionManager getMacroExpansionManager(@NotNull Project project) {
        return project.getService(MacroExpansionManager.class);
    }

    @NotNull
    public static MacroExpansionManager getMacroExpansionManagerIfCreated(@NotNull Project project) {
        MacroExpansionManager service = project.getServiceIfCreated(MacroExpansionManager.class);
        if (service != null) return service;
        return getMacroExpansionManager(project);
    }

    @NotNull
    public static Path getBaseMacroDir() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, ".intellij-rust", "macros");
    }

    @NotNull
    public static String expansionNameToPath(@NotNull String expansionName) {
        return expansionName.replace('.', '/') + ".rs";
    }
}
