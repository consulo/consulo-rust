/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplatePsiInfo;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.ty.TyBool;
import org.rust.lang.utils.NegateUtil;
import org.rust.lang.core.types.RsTypesUtil;

public final class PostfixUtil {

    private PostfixUtil() {
    }

    public static final PostfixTemplatePsiInfo RsPostfixTemplatePsiInfo = new PostfixTemplatePsiInfo() {
        @NotNull
        @Override
        public PsiElement getNegatedExpression(@NotNull PsiElement element) {
            return NegateUtil.negate(element);
        }

        @NotNull
        @Override
        public PsiElement createExpression(@NotNull PsiElement context, @NotNull String prefix, @NotNull String suffix) {
            return new RsPsiFactory(context.getProject()).createExpression(prefix + context.getText() + suffix);
        }
    };

    public static boolean isBool(@NotNull RsExpr expr) {
        return RsTypesUtil.getType(expr) instanceof TyBool;
    }
}
