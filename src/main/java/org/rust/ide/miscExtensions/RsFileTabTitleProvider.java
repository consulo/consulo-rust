/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.miscExtensions;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.fileEditor.UniqueVFilePathBuilder;
import com.intellij.openapi.fileEditor.impl.UniqueNameEditorTabTitleProvider;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.RsFile;

import java.util.Set;

public class RsFileTabTitleProvider extends UniqueNameEditorTabTitleProvider {

    private static final Set<String> EXPLICIT_FILES = Set.of(
        RsConstants.MOD_RS_FILE,
        RsConstants.LIB_RS_FILE,
        RsConstants.MAIN_RS_FILE
    );

    @Nullable
    @Override
    public String getEditorTabTitle(@NotNull Project project, @NotNull VirtualFile file) {
        if (!((RsFile.isRustFile(file) && EXPLICIT_FILES.contains(file.getName())) || CargoConstants.MANIFEST_FILE.equals(file.getName()))) {
            return null;
        }

        UISettings uiSettings = UISettings.getInstanceOrNull();
        if (uiSettings == null || !uiSettings.getShowDirectoryForNonUniqueFilenames() || DumbService.isDumb(project)) {
            return null;
        }

        String uniqueName = UniqueVFilePathBuilder.getInstance().getUniqueVirtualFilePath(project, file);
        if (uniqueName.equals(file.getName())) {
            return null;
        }
        return uniqueName;
    }
}
