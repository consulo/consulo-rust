/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix.editable;

import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.TextExpression;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.editable.EditablePostfixTemplateWithMultipleExpressions;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
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

        return ContainerUtil.filter(expressions, Conditions.and(
            (Condition<PsiElement>) e -> PSI_ERROR_FILTER.value(e) && e instanceof RsExpr && e.getTextRange().getEndOffset() == offset,
            getExpressionCompositeCondition()
        ));
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

    public static TemplateImpl createTemplate(String templateText) {
        TemplateImpl template = new TemplateImpl("fakeKey", templateText, "");
        template.setToReformat(false);
        template.parseSegments();

        for (int i = 0; i < template.getSegmentsCount(); i++) {
            String segmentName = template.getSegmentName(i);
            boolean internalName = segmentName.equals("EXPR")
                || segmentName.equals(TemplateImpl.ARG)
                || TemplateImpl.INTERNAL_VARS_SET.contains(segmentName);
            if (!internalName) {
                template.addVariable(segmentName, new TextExpression(segmentName), true);
            }
        }

        return template;
    }
}
