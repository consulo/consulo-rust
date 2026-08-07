/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.convertStruct;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.ui.RefactoringDialog;
import consulo.ui.ex.awt.JBCheckBox;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsBaseEditorRefactoringAction;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFieldsOwner;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.awt.*;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionRef;

@ActionImpl(
    id = "Rust.RsConvertToTuple",
    parents = @ActionParentRef(
        value = @ActionRef(id = "RefactoringMenu")
    )
)
public class RsConvertToTupleAction extends RsBaseEditorRefactoringAction {

    @Override
    public boolean isAvailableOnElementInEditorAndFile(
        @Nonnull PsiElement element,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull DataContext context
    ) {
        RsFieldsOwner owner = RsElementUtil.ancestorOrSelf(element, RsFieldsOwner.class);
        if (owner == null) return false;
        return owner.getBlockFields() != null;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nullable DataContext dataContext) {
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
        @Nonnull
        private final RsFieldsOwner myElement;
        @Nonnull
        private final JBCheckBox myCb;

        Dialog(@Nonnull Project project, @Nonnull RsFieldsOwner element) {
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

        @Nonnull
        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout(2, 2));
            panel.setPreferredSize(new Dimension(300, 100));
            panel.add(myCb);
            return panel;
        }
    }
}
