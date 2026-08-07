/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsPatFieldFull;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsVisitor;

import java.util.Collections;
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsNonShorthandFieldPatternsInspection extends RsLintInspection {

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.NonShorthandFieldPatterns;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitPatFieldFull(@Nonnull RsPatFieldFull o) {
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

        UseShorthandFieldPatternFix(@Nonnull RsPatFieldFull element, @Nonnull String identifier) {
            super(element);
            myIdentifier = identifier;
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.use.shorthand.field.pattern"));
            }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.use.shorthand.field.pattern", myIdentifier));
            }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsPatFieldFull element) {
            RsPatBinding patBinding = new RsPsiFactory(project).createPatBinding(element.getPat().getText());
            element.getParent().addBefore(patBinding, element);
            element.delete();
        }
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.non.shorthand.field.patterns.display.name"));
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

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WEAK_WARNING;
    }
}
