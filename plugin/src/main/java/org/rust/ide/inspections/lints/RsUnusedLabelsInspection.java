/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import jakarta.annotation.Nonnull;
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

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.UnusedLabels;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitLabelDecl(@Nonnull RsLabelDecl o) {
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

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.unused.labels.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("lints"));
    }
}
