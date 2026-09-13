package com.intellij.codeInsight.template.postfix.templates.editable;
import consulo.disposer.Disposable;
import javax.swing.JComponent;
public interface PostfixTemplateEditor extends Disposable {
    JComponent getComponent();
    Object createTemplate(String templateId, String templateName);
}
