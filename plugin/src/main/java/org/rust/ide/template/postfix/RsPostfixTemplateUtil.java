/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import consulo.language.editor.postfixTemplate.PostfixTemplatePsiInfo;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.utils.RsBooleanExpUtils;

import java.util.function.Predicate;

public final class RsPostfixTemplateUtil {
    private RsPostfixTemplateUtil() {}

    @Nonnull
    public static final PostfixTemplatePsiInfo RS_POSTFIX_TEMPLATE_PSI_INFO = new PostfixTemplatePsiInfo() {
        @Override
        public @Nonnull PsiElement createExpression(@Nonnull PsiElement context, @Nonnull String prefix, @Nonnull String suffix) {
            return new RsPsiFactory(context.getProject()).createExpression(prefix + context.getText() + suffix);
        }

        @Override
        public @Nonnull PsiElement getNegatedExpression(@Nonnull PsiElement element) {
            return RsBooleanExpUtils.negate(element);
        }
    };

    @Nonnull
    public static Predicate<PsiElement> RS_EXPR_CONDITION = psi -> psi instanceof RsExpr;
}
