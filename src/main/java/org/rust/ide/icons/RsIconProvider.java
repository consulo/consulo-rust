/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.icons;

import com.intellij.ide.IconProvider;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.RsFile;

import javax.swing.*;

public class RsIconProvider extends IconProvider {
    @Nullable
    @Override
    public Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof RsFile) {
            return getFileIcon((RsFile) element);
        }
        return null;
    }

    @Nullable
    private Icon getFileIcon(@NotNull RsFile file) {
        if (file.getName().equals(RsConstants.MOD_RS_FILE)) {
            return RsIcons.MOD_RS;
        }
        if (isMainFile(file)) {
            return RsIcons.MAIN_RS;
        }
        if (isBuildRs(file)) {
            return CargoIcons.BUILD_RS_ICON;
        }
        return null;
    }

    private boolean isMainFile(@NotNull RsFile element) {
        return (element.getName().equals(RsConstants.MAIN_RS_FILE) || element.getName().equals(RsConstants.LIB_RS_FILE))
            && element.isCrateRoot();
    }

    private boolean isBuildRs(@NotNull RsFile element) {
        // TODO containingTarget
        return element.isCrateRoot() && element.getCrate().getKind() == CargoWorkspace.TargetKind.CustomBuild.INSTANCE;
    }
}
