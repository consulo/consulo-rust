/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.convertStruct;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsBaseEditorRefactoringAction;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFieldsOwner;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.awt.*;

public class RsConvertToTupleAction extends RsBaseEditorRefactoringAction {

    @Override
    public boolean isAvailableOnElementInEditorAndFile(
        @NotNull PsiElement element,
        @NotNull Editor editor,
        @NotNull PsiFile file,
        @NotNull DataContext context
    ) {
        RsFieldsOwner owner = RsElementUtil.ancestorOrSelf(element, RsFieldsOwner.class);
        if (owner == null) return false;
        return owner.getBlockFields() != null;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @Nullable DataContext dataContext) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement found = file.findElementAt(offset);
        if (found == null) return;
        RsFieldsOwner element = RsElementUtil.ancestorOrSelf(found, RsFieldsOwner.class);
        if (element == null) return;

        if (org.rust.openapiext.OpenApiUtil.isHeadlessEnvironment()) {
            RsConvertToTupleProcessor processor = new RsConvertToTupleProcessor(project, element, true);
            processor.setPreviewUsages(false);
            processor.run();
        } else {
            new Dialog(project, element).show();
        }
    }

    private static class Dialog extends RefactoringDialog {
        @NotNull
        private final RsFieldsOwner myElement;
        @NotNull
        private final JBCheckBox myCb;

        Dialog(@NotNull Project project, @NotNull RsFieldsOwner element) {
            super(project, false);
            myElement = element;
            myCb = new JBCheckBox(RsBundle.message("checkbox.convert.all.usages"), true);
            super.init();
            setTitle(RsBundle.message("dialog.title.convert.to.tuple"));
        }

        @Override
        protected void doAction() {
            invokeRefactoring(new RsConvertToTupleProcessor(getProject(), myElement, myCb.isSelected()));
        }

        @NotNull
        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout(2, 2));
            panel.setPreferredSize(new Dimension(300, 100));
            panel.add(myCb);
            return panel;
        }
    }
}
