/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsPatFieldFull;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsVisitor;

import java.util.Collections;

public class RsNonShorthandFieldPatternsInspection extends RsLintInspection {

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.NonShorthandFieldPatterns;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitPatFieldFull(@NotNull RsPatFieldFull o) {
                PsiElement identifierElement = o.getIdentifier();
                if (identifierElement == null) return;
                String identifier = identifierElement.getText();
                String binding = o.getPat().getText();
                if (!identifier.equals(binding)) return;

                registerLintProblem(
                    holder,
                    o,
                    RsBundle.message("inspection.message.in.this.pattern.redundant", identifier),
                    RsLintHighlightingType.WEAK_WARNING,
                    Collections.singletonList(new UseShorthandFieldPatternFix(o, identifier))
                );
            }
        };
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }

    private static class UseShorthandFieldPatternFix extends RsQuickFixBase<RsPatFieldFull> {
        private final String myIdentifier;

        UseShorthandFieldPatternFix(@NotNull RsPatFieldFull element, @NotNull String identifier) {
            super(element);
            myIdentifier = identifier;
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return RsBundle.message("intention.family.name.use.shorthand.field.pattern");
        }

        @NotNull
        @Override
        public String getText() {
            return RsBundle.message("intention.name.use.shorthand.field.pattern", myIdentifier);
        }

        @Override
        public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsPatFieldFull element) {
            RsPatBinding patBinding = new RsPsiFactory(project).createPatBinding(element.getPat().getText());
            element.getParent().addBefore(patBinding, element);
            element.delete();
        }
    }
}
