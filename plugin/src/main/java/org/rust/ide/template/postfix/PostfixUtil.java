/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import consulo.language.editor.postfixTemplate.PostfixTemplatePsiInfo;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.ty.TyBool;
import org.rust.lang.utils.NegateUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.psi.impl.*;

public final class PostfixUtil {

    private PostfixUtil() {
    }

    public static final PostfixTemplatePsiInfo RsPostfixTemplatePsiInfo = new PostfixTemplatePsiInfo() {
        @Nonnull
        @Override
        public PsiElement getNegatedExpression(@Nonnull PsiElement element) {
            return NegateUtil.negate(element);
        }

        @Nonnull
        @Override
        public PsiElement createExpression(@Nonnull PsiElement context, @Nonnull String prefix, @Nonnull String suffix) {
            return new RsPsiFactory(context.getProject()).createExpression(prefix + context.getText() + suffix);
        }
    };

    public static boolean isBool(@Nonnull RsExpr expr) {
        return RsTypesUtil.getType(expr) instanceof TyBool;
    }
}
