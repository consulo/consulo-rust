/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.fixes.NameSuggestionFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsLitExprUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RsUnknownCrateTypesInspection extends RsLintInspection {

    public static final List<String> KNOWN_CRATE_TYPES = Arrays.asList(
        "bin", "lib", "dylib", "staticlib", "cdylib", "rlib", "proc-macro"
    );

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.UnknownCrateTypes;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitLitExpr(@Nonnull RsLitExpr element) {
                if (!RsPsiPattern.insideCrateTypeAttrValue.accepts(element)) return;

                String elementValue = RsLitExprUtil.getStringValue(element);
                if (elementValue == null) return;
                if (!KNOWN_CRATE_TYPES.contains(elementValue)) {
                    List<? extends consulo.language.editor.inspection.LocalQuickFix> fixes = NameSuggestionFix.createApplicable(
                        element, elementValue, KNOWN_CRATE_TYPES, 1,
                        it -> new RsPsiFactory(element.getProject()).createExpression("\"" + it + "\"")
                    );

                    registerLintProblem(holder, element, RsBundle.message("inspection.message.invalid.crate.type.value"),
                        RsLintHighlightingType.DEFAULT, Collections.unmodifiableList(fixes));
                }
            }
        };
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.unknown.crate.types.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("lints"));
    }
}
