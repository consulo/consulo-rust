/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix;

import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateExpressionSelectorBase;
import consulo.document.Document;
import consulo.util.lang.function.Condition;
import consulo.language.psi.PsiElement;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsExpr;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

public class RsExprParentsSelector extends PostfixTemplateExpressionSelectorBase {

    public RsExprParentsSelector() {
        this(expr -> true);
    }

    public RsExprParentsSelector(Predicate<RsExpr> pred) {
        super((Condition<PsiElement>) element -> element instanceof RsExpr && pred.test((RsExpr) element));
    }

    @Override
    public List<PsiElement> getExpressions(PsiElement context, Document document, int offset) {
        List<PsiElement> expressions = super.getExpressions(context, document, offset);
        if (OpenApiUtil.isUnitTestMode()) {
            if (expressions.isEmpty()) return Collections.emptyList();
            return Collections.singletonList(expressions.get(expressions.size() - 1));
        }
        return expressions;
    }

    @Override
    protected List<PsiElement> getNonFilteredExpressions(PsiElement context, Document document, int offset) {
        List<PsiElement> result = new ArrayList<>();
        PsiElement current = context;
        while (current != null && !(current instanceof RsBlock)) {
            if (current instanceof RsExpr) {
                result.add(current);
            }
            current = current.getParent();
        }
        return result;
    }
}
