/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.RemoveElementFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsLabelDecl;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsLabelDeclExtUtil;

import java.util.Collections;

/** Analogue of rustc's unused_labels. */
public class RsUnusedLabelsInspection extends RsLintInspection {

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.UnusedLabels;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitLabelDecl(@NotNull RsLabelDecl o) {
                boolean isLabelUsed = ReferencesSearch.search(
                    o,
                    new LocalSearchScope(RsLabelDeclExtUtil.getOwner(o)),
                    /*ignoreAccessScope=*/ true
                ).findFirst() != null;

                if (!isLabelUsed) {
                    RsLintHighlightingType highlighting = RsLintHighlightingType.UNUSED_SYMBOL;
                    String description = RsBundle.message("inspection.UnusedLabels.description");
                    registerLintProblem(holder, o, description, highlighting, Collections.singletonList(new RemoveElementFix(o)));
                }
            }
        };
    }
}
