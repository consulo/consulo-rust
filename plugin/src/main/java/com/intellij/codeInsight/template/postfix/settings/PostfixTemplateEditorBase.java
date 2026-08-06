package com.intellij.codeInsight.template.postfix.settings;
import com.intellij.codeInsight.template.postfix.templates.editable.PostfixTemplateEditor;
import consulo.disposer.Disposable;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
/** IntelliJ-compat stub. */
public abstract class PostfixTemplateEditorBase<T> implements PostfixTemplateEditor {
    protected T myProvider;
    protected JPanel myEditTemplateAndConditionsPanel = new JPanel();
    protected JBCheckBox myApplyToTheTopmostJBCheckBox = new JBCheckBox();
    protected DefaultListModel<T> myExpressionTypesListModel = new DefaultListModel<>();
    protected Object myTemplateEditor;
    public PostfixTemplateEditorBase(T provider, Object template, boolean useTopmost) { this.myProvider = provider; }
    public PostfixTemplateEditorBase(Object provider, Object template) { /* overload */ }
    @Override public JComponent getComponent() { return myEditTemplateAndConditionsPanel; }
    @Override public Object createTemplate(String templateId, String templateName) { return null; }
    @Override public void dispose() {}

    public abstract static class AddConditionAction extends AnAction {
        @Override public void actionPerformed(@Nonnull AnActionEvent e) {}
    }
}
