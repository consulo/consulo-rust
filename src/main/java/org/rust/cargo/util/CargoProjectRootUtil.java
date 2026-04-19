/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.rust.cargo.CargoConstants;

/**
 * Extracts Cargo based project's root-path (the one containing {@code Cargo.toml})
 */
public final class CargoProjectRootUtil {

    private CargoProjectRootUtil() {
    }

    public static VirtualFile getCargoProjectRoot(Module module) {
        for (VirtualFile root : ModuleRootManager.getInstance(module).getContentRoots()) {
            if (root.findChild(CargoConstants.MANIFEST_FILE) != null) {
                return root;
            }
        }
        return null;
    }
}
