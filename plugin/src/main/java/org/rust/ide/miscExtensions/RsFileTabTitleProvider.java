/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.miscExtensions;

import consulo.application.ui.UISettings;
import consulo.fileEditor.EditorTabTitleProvider;
import consulo.fileEditor.UniqueVFilePathBuilder;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.RsFile;

import java.util.Set;

public class RsFileTabTitleProvider implements EditorTabTitleProvider {

    private static final Set<String> EXPLICIT_FILES = Set.of(
        RsConstants.MOD_RS_FILE,
        RsConstants.LIB_RS_FILE,
        RsConstants.MAIN_RS_FILE
    );

    @Nullable
    @Override
    public String getEditorTabTitle(@Nonnull Project project, @Nonnull VirtualFile file) {
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
