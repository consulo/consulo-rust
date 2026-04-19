/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsEmptyStmt;
import org.rust.lang.core.psi.RsStmt;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.ext.RsPsiElementUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsPsiElementExtUtil;

/** Analogue of https://doc.rust-lang.org/rustc/lints/listing/warn-by-default.html#redundant-semicolons */
public class RsRedundantSemicolonsInspection extends RsLintInspection {

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.RedundantSemicolons;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitBlock(@NotNull RsBlock block) {
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
        @NotNull List<RsStmt> stmts,
        @NotNull RsBlock block,
        @NotNull RsProblemsHolder holder
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

        FixRedundantSemicolons(@NotNull PsiElement element) {
            super(element);
        }

        @NotNull
        @Override
        public String getText() {
            return RsBundle.message("inspection.RedundantSemicolons.fix.name");
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getText();
        }

        @Override
        public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
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
}
