/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplatePsiInfo;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.utils.RsBooleanExpUtils;

import java.util.function.Predicate;

public final class RsPostfixTemplateUtil {
    private RsPostfixTemplateUtil() {}

    @NotNull
    public static final PostfixTemplatePsiInfo RS_POSTFIX_TEMPLATE_PSI_INFO = new PostfixTemplatePsiInfo() {
        @Override
        public @NotNull PsiElement createExpression(@NotNull PsiElement context, @NotNull String prefix, @NotNull String suffix) {
            return new RsPsiFactory(context.getProject()).createExpression(prefix + context.getText() + suffix);
        }

        @Override
        public @NotNull PsiElement getNegatedExpression(@NotNull PsiElement element) {
            return RsBooleanExpUtils.negate(element);
        }
    };

    @NotNull
    public static Predicate<PsiElement> RS_EXPR_CONDITION = psi -> psi instanceof RsExpr;
}
