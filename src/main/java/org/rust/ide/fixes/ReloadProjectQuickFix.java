/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.ide.notifications.TrustedProjectNotificationUtil;
import org.rust.openapiext.SaveAllDocumentsUtil;

public class ReloadProjectQuickFix implements LocalQuickFix {

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.reload.project");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        if (!TrustedProjectNotificationUtil.confirmLoadingUntrustedProject(project)) return;
        SaveAllDocumentsUtil.saveAllDocuments();
        CargoProjectServiceUtil.getCargoProjects(project).refreshAllProjects();
    }
}
