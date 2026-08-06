/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.icons;

import com.intellij.ide.IconProvider;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.RsFile;

import javax.swing.*;

public class RsIconProvider extends IconProvider {
    @Nullable
    @Override
    public consulo.ui.image.Image getIcon(@Nonnull PsiElement element, int flags) {
        if (element instanceof RsFile) {
            return getFileIcon((RsFile) element);
        }
        return null;
    }

    @Nullable
    private consulo.ui.image.Image getFileIcon(@Nonnull RsFile file) {
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

    private boolean isMainFile(@Nonnull RsFile element) {
        return (element.getName().equals(RsConstants.MAIN_RS_FILE) || element.getName().equals(RsConstants.LIB_RS_FILE))
            && element.isCrateRoot();
    }

    private boolean isBuildRs(@Nonnull RsFile element) {
        // TODO containingTarget
        return element.isCrateRoot() && element.getCrate().getKind() == CargoWorkspace.TargetKind.CustomBuild.INSTANCE;
    }
}
