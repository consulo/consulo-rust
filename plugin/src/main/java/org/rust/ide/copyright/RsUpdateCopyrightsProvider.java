/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.copyright;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.copyright.UpdateCopyrightsProvider;
import consulo.language.copyright.UpdatePsiFileCopyright;
import consulo.language.copyright.config.CopyrightFileConfig;
import consulo.language.copyright.config.CopyrightProfile;
import consulo.language.copyright.ui.TemplateCommentPanel;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsFileType;

/**
 * Lets the copyright profile insert and update the copyright comment of a Rust file.
 */
@ExtensionImpl
public class RsUpdateCopyrightsProvider extends UpdateCopyrightsProvider<CopyrightFileConfig> {
    @Override
    public FileType getFileType() {
        return RsFileType.INSTANCE;
    }

    @Override
    public UpdatePsiFileCopyright<CopyrightFileConfig> createInstance(@Nonnull PsiFile file,
                                                                     CopyrightProfile copyrightProfile) {
        return new UpdatePsiFileCopyright<>(file, copyrightProfile) {
            @Override
            protected void scanFile() {
                // The copyright comment belongs at the very top, before any item or inner attribute.
                checkComments(file.getFirstChild(), null, true);
            }
        };
    }

    @Override
    public CopyrightFileConfig createDefaultOptions() {
        return new CopyrightFileConfig();
    }

    @Override
    public TemplateCommentPanel createConfigurable(Project project, TemplateCommentPanel parentPane, FileType fileType) {
        return new TemplateCommentPanel(fileType, parentPane, project);
    }
}
