/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.icons;

import com.intellij.ide.FileIconProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.rust.cargo.CargoConstants;

import javax.swing.*;

public class CargoIconProvider implements FileIconProvider {
    @Override
    public Icon getIcon(VirtualFile file, int flags, Project project) {
        String name = file.getName();
        if (CargoConstants.MANIFEST_FILE.equals(name)) {
            return CargoIcons.ICON;
        } else if (CargoConstants.XARGO_MANIFEST_FILE.equals(name)) {
            return CargoIcons.ICON;
        } else if (CargoConstants.LOCK_FILE.equals(name)) {
            return CargoIcons.LOCK_ICON;
        } else {
            return null;
        }
    }
}
