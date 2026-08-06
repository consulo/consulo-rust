/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.core.FeatureAvailability;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemsOwnerUtil;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

public class RsMainFunctionNotFoundInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsVisitor() {
            @Override
            public void visitFile(@Nonnull PsiFile file) {
                if (file instanceof RsFile) {
                    RsFile rsFile = (RsFile) file;
                    if (RsPsiJavaUtil.childOfType(rsFile, PsiErrorElement.class) != null) return;

                    Crate crate = Crate.asNotFake(rsFile.getCrate());
                    if (crate == null) return;
                    if (!crate.getKind().canHaveMainFunction()) return;
                    if (!rsFile.isCrateRoot()) return;

                    if (rsFile.getQueryAttributes().hasAttribute("no_main")) return;
                    if (CompilerFeature.getSTART().availability(rsFile) == FeatureAvailability.AVAILABLE) return;
                    boolean hasMainFunction = RsItemsOwnerUtil.processExpandedItemsExceptImplsAndUses(rsFile, it -> it instanceof RsFunction && "main".equals(((RsFunction) it).getName()));
                    if (!hasMainFunction) {
                        new RsDiagnostic.MainFunctionNotFound(rsFile, crate.getPresentableName()).addToHolder(holder);
                    }
                }
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.main.function.not.found.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
