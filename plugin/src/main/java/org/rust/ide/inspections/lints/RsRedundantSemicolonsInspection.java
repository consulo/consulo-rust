/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsEmptyStmt;
import org.rust.lang.core.psi.RsStmt;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.ext.impl.RsPsiElementUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.impl.RsPsiElementExtUtil;
import consulo.localize.LocalizeValue;
import consulo.annotation.component.ExtensionImpl;

/** Analogue of https://doc.rust-lang.org/rustc/lints/listing/warn-by-default.html#redundant-semicolons */
@ExtensionImpl
public class RsRedundantSemicolonsInspection extends RsLintInspection {

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.RedundantSemicolons;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitBlock(@Nonnull RsBlock block) {
                super.visitBlock(block);

                List<RsStmt> seq = new ArrayList<>();
                for (PsiElement element : block.getChildren()) {
                    if (element instanceof RsEmptyStmt) {
                        seq.add((RsStmt) element);
                    } else if (element instanceof RsItemElement || element instanceof RsStmt) {
                        tryRegisterProblemAndClearSeq(seq, block, holder);
                    }
                }
                tryRegisterProblemAndClearSeq(seq, block, holder);
            }
        };
    }

    private void tryRegisterProblemAndClearSeq(
        @Nonnull List<RsStmt> stmts,
        @Nonnull RsBlock block,
        @Nonnull RsProblemsHolder holder
    ) {
        if (stmts.isEmpty()) return;
        RsLintHighlightingType highlighting = RsLintHighlightingType.UNUSED_SYMBOL;
        if (stmts.size() == 1) {
            String description = RsBundle.message("inspection.RedundantSemicolons.description.single");
            List<FixRedundantSemicolons> fixes = Collections.singletonList(new FixRedundantSemicolons(stmts.get(0)));
            registerLintProblem(holder, stmts.get(0), description, highlighting, Collections.unmodifiableList(fixes));
        } else {
            String description = RsBundle.message("inspection.RedundantSemicolons.description.multiple");
            TextRange range = TextRange.create(
                stmts.get(0).getStartOffsetInParent(),
                RsPsiElementExtUtil.getEndOffsetInParent(stmts.get(stmts.size() - 1))
            );
            List<FixRedundantSemicolons> fixes = Collections.singletonList(new FixRedundantSemicolons(stmts.get(0)));
            registerLintProblem(holder, block, description, range, highlighting, Collections.unmodifiableList(fixes));
        }
        stmts.clear();
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }

    private static class FixRedundantSemicolons extends RsQuickFixBase<PsiElement> {

        FixRedundantSemicolons(@Nonnull PsiElement element) {
            super(element);
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("inspection.RedundantSemicolons.fix.name"));
            }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return getText();
            }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
            PsiElement parent = element.getParent();
            if (!(parent instanceof RsBlock)) return;
            PsiElement last = null;
            PsiElement sibling = element.getNextSibling();
            while (sibling != null && (sibling instanceof RsEmptyStmt || sibling instanceof PsiWhiteSpace)) {
                last = sibling;
                sibling = sibling.getNextSibling();
            }
            parent.deleteChildRange(element, last != null ? last : element);
        }
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.redundant.semicolons.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("lints"));
    }

    @Nonnull
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.of(RsBundle.message("rust"))};
    }
}
