/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateExpressionSelectorBase;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplatePsiInfo;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyBool;
import org.rust.lang.utils.RsNegateUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import org.rust.lang.utils.RsBooleanExpUtils;

public final class RsPostfixTemplateUtils {

    public static final PostfixTemplatePsiInfo RS_POSTFIX_TEMPLATE_PSI_INFO = new PostfixTemplatePsiInfo() {
        @Override
        public PsiElement getNegatedExpression(PsiElement element) {
            return RsBooleanExpUtils.negate(element);
        }

        @Override
        public PsiElement createExpression(PsiElement context, String prefix, String suffix) {
            return new RsPsiFactory(context.getProject()).createExpression(prefix + context.getText() + suffix);
        }
    };

    private RsPostfixTemplateUtils() {
    }

    public static boolean isBool(RsExpr expr) {
        return RsTypesUtil.getType(expr) instanceof TyBool;
    }
}
