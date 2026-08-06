package com.intellij.codeInsight.template.postfix.templates.editable;
import consulo.language.editor.refactoring.postfixTemplate.PostfixTemplateExpressionCondition;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.Collections;
import java.util.List;
public class DefaultPostfixTemplateEditor implements PostfixTemplateEditor {
    public DefaultPostfixTemplateEditor(Object provider, Object template) {}
    @Override public JComponent getComponent() { return new JPanel(); }
    @Override public Object createTemplate(String templateId, String templateName) { return null; }
    public List<PostfixTemplateExpressionCondition<consulo.language.psi.PsiElement>> getExpressionConditions() { return Collections.emptyList(); }
    @Override public void dispose() {}
}
