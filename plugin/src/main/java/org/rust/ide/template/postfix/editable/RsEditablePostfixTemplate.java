/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix.editable;

import consulo.language.editor.template.TextExpression;
import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.refactoring.postfixTemplate.EditablePostfixTemplateWithMultipleExpressions;
import consulo.document.Document;
import consulo.project.DumbService;
import consulo.util.lang.function.Condition;
import consulo.util.lang.function.Conditions;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.ContainerUtil;
import org.rust.ide.template.postfix.RsExprParentsSelector;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsExprStmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RsEditablePostfixTemplate extends EditablePostfixTemplateWithMultipleExpressions<RsPostfixTemplateExpressionCondition> {

    private static final Condition<PsiElement> PSI_ERROR_FILTER =
        element -> element != null && !PsiTreeUtil.hasErrorElements(element);

    public RsEditablePostfixTemplate(
        String templateId,
        String templateName,
        String templateText,
        String example,
        Set<RsPostfixTemplateExpressionCondition> expressionTypes,
        boolean useTopmostExpression,
        PostfixTemplateProvider provider
    ) {
        super(templateId, templateName, createTemplate(templateText), example, expressionTypes,
            useTopmostExpression, provider);
    }

    @Override
    protected List<PsiElement> getExpressions(PsiElement context, Document document, int offset) {
        if (DumbService.getInstance(context.getProject()).isDumb()) return new ArrayList<>();

        List<PsiElement> allExpressions = new RsExprParentsSelector().getExpressions(context, document, offset);
        List<PsiElement> expressions;
        if (myUseTopmostExpression) {
            PsiElement topmost = null;
            int maxLen = -1;
            for (PsiElement e : allExpressions) {
                if (e.getTextLength() > maxLen) {
                    maxLen = e.getTextLength();
                    topmost = e;
                }
            }
            if (topmost == null) return new ArrayList<>();
            expressions = new ArrayList<>();
            expressions.add(topmost);
        } else {
            expressions = allExpressions;
        }

        if (myExpressionConditions.isEmpty() && context instanceof RsExpr) {
            return new ArrayList<>(expressions);
        }

        Condition<PsiElement> localCondition = e -> PSI_ERROR_FILTER.value(e) && e instanceof RsExpr && e.getTextRange().getEndOffset() == offset;
        java.util.function.Predicate<PsiElement> composite = getExpressionCompositeCondition();
        return ContainerUtil.filter(expressions, (Condition<PsiElement>) e -> localCondition.value(e) && composite.test(e));
    }

    @Override
    protected PsiElement getTopmostExpression(PsiElement element) {
        if (element.getParent() instanceof RsExprStmt) return element.getParent();
        return element;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    // createTemplate(String) is inherited from EditablePostfixTemplateWithMultipleExpressions,
    // which does the same segment parsing the local copy did against the internal TemplateImpl.
}
