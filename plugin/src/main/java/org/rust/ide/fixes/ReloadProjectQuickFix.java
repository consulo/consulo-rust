/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.openapiext.SaveAllDocumentsUtil;

public class ReloadProjectQuickFix implements LocalQuickFix {

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.reload.project"));
    }

    @Override
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        SaveAllDocumentsUtil.saveAllDocuments();
        CargoProjectServiceUtil.getCargoProjects(project).refreshAllProjects();
    }
}
