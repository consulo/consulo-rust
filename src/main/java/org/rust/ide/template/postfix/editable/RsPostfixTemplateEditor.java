/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.template.postfix.editable;

import com.intellij.codeInsight.template.impl.TemplateEditorUtil;
import com.intellij.codeInsight.template.postfix.settings.PostfixTemplateEditorBase;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UIUtil;
import org.rust.RsBundle;
import org.rust.ide.template.postfix.RsPostfixTemplateProvider;

import javax.swing.*;
import java.util.LinkedHashSet;
import java.util.Set;

public class RsPostfixTemplateEditor extends PostfixTemplateEditorBase<RsPostfixTemplateExpressionCondition> {

    private JPanel myPanel;

    public RsPostfixTemplateEditor(RsPostfixTemplateProvider provider) {
        super(provider, createEditor(), true);
        myPanel = FormBuilder.createFormBuilder()
            .addComponentFillVertically(myEditTemplateAndConditionsPanel, UIUtil.DEFAULT_VGAP)
            .getPanel();
    }

    @Override
    public PostfixTemplate createTemplate(String templateId, String templateName) {
        Set<RsPostfixTemplateExpressionCondition> types = new LinkedHashSet<>();
        java.util.Enumeration<RsPostfixTemplateExpressionCondition> elements = myExpressionTypesListModel.elements();
        while (elements.hasMoreElements()) {
            types.add(elements.nextElement());
        }
        String templateText = myTemplateEditor.getDocument().getText();
        boolean useTopmostExpression = myApplyToTheTopmostJBCheckBox.isSelected();

        return new RsEditablePostfixTemplate(templateId, templateName, templateText, "", types, useTopmostExpression, myProvider);
    }

    @Override
    public JComponent getComponent() {
        return myPanel;
    }

    @Override
    protected void fillConditions(DefaultActionGroup group) {
        for (RsPostfixTemplateExpressionCondition.Type type : RsPostfixTemplateExpressionCondition.Type.values()) {
            if (type != RsPostfixTemplateExpressionCondition.Type.UserEntered) {
                group.add(new AddConditionAction(new RsPostfixTemplateExpressionCondition(type)));
            }
        }
        group.add(new EnterCustomTypeNameAction());
    }

    private class EnterCustomTypeNameAction extends DumbAwareAction {
        EnterCustomTypeNameAction() {
            super(RsBundle.message("action.enter.type.name.text"));
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            String typeName = Messages.showInputDialog(
                myPanel,
                RsBundle.message("dialog.message.enter.custom.type.name.type.parameters.are.not.supported"),
                RsBundle.message("dialog.title.enter.type.name"),
                null
            );
            if (typeName != null) {
                RsPostfixTemplateExpressionCondition userEnteredType =
                    new RsPostfixTemplateExpressionCondition(RsPostfixTemplateExpressionCondition.Type.UserEntered, typeName);
                myExpressionTypesListModel.addElement(userEnteredType);
            }
        }
    }

    public static Editor createEditor() {
        com.intellij.openapi.project.Project project = ProjectManager.getInstance().getDefaultProject();
        com.intellij.openapi.editor.Document document = EditorFactory.getInstance().createDocument("");
        return TemplateEditorUtil.createEditor(false, document, project);
    }
}
