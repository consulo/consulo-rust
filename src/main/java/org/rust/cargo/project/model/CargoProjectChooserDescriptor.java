/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;

public class CargoProjectChooserDescriptor extends FileChooserDescriptor {

    public static final CargoProjectChooserDescriptor INSTANCE = new CargoProjectChooserDescriptor();

    private CargoProjectChooserDescriptor() {
        super(true, true, false, false, false, false);
        withFileFilter(file -> AttachCargoProjectAction.isCargoToml(file));
        withTitle(RsBundle.message("dialog.title.select.cargo.toml"));
    }

    @Override
    public boolean isFileSelectable(@Nullable VirtualFile file) {
        return super.isFileSelectable(file) && file != null &&
            (!file.isDirectory() || file.findChild(CargoConstants.MANIFEST_FILE) != null);
    }
}
