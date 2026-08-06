/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.convertStruct;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.project.Project;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.ui.RefactoringDialog;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.GridBag;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsBaseEditorRefactoringAction;
import org.rust.ide.refactoring.RsNamesValidator;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFieldsOwner;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RsConvertToNamedFieldsAction extends RsBaseEditorRefactoringAction {

    @Override
    public boolean isAvailableOnElementInEditorAndFile(
        @Nonnull PsiElement element,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull DataContext context
    ) {
        RsFieldsOwner owner = RsElementUtil.ancestorOrSelf(element, RsFieldsOwner.class);
        if (owner == null) return false;
        return owner.getTupleFields() != null;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nullable DataContext dataContext) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement found = file.findElementAt(offset);
        if (found == null) return;
        RsFieldsOwner element = RsElementUtil.ancestorOrSelf(found, RsFieldsOwner.class);
        if (element == null) return;

        if (org.rust.openapiext.OpenApiUtil.isHeadlessEnvironment()) {
            RsConvertToNamedFieldsProcessor processor = new RsConvertToNamedFieldsProcessor(project, element, true);
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
        @Nonnull
        private final List<EditorTextField> myEditors;

        Dialog(@Nonnull Project project, @Nonnull RsFieldsOwner element) {
            super(project, false);
            myElement = element;
            myCb = new JBCheckBox(RsBundle.message("checkbox.convert.all.usages"), true);
            int count = element.getTupleFields().getTupleFieldDeclList().size() + 1;
            myEditors = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                EditorTextField editor = new EditorTextField("_" + i);
                int idx = i;
                editor.addDocumentListener(new DocumentListener() {
                    @Override
                    public void documentChanged(@Nonnull DocumentEvent event) {
                        updateErrorInfo(doValidateAll());
                    }
                });
                editor.selectAll();
                myEditors.add(editor);
            }
            super.init();
            setTitle(RsBundle.message("dialog.title.convert.to.named.fields.settings"));
        }

        @Nonnull
        @Override
        protected List<ValidationInfo> doValidateAll() {
            getRefactorAction().setEnabled(true);
            List<ValidationInfo> errors = new ArrayList<>();
            for (EditorTextField editor : myEditors) {
                if (!RsNamesValidator.isValidRustVariableIdentifier(editor.getText())) {
                    getRefactorAction().setEnabled(false);
                    errors.add(new ValidationInfo(RsBundle.message("dialog.message.invalid.identifier"), editor));
                }
            }
            return errors;
        }

        @Override
        protected void doAction() {
            List<String> names = myEditors.stream().map(EditorTextField::getText).collect(Collectors.toList());
            invokeRefactoring(new RsConvertToNamedFieldsProcessor(getProject(), myElement, myCb.isSelected(), names));
        }

        @Nonnull
        @Override
        protected JComponent createCenterPanel() {
            JPanel panel = new JPanel(new BorderLayout(2, 2));
            panel.setPreferredSize(new Dimension(400, 200));

            JPanel gridPanel = new JPanel(new GridBagLayout());
            GridBag gridBuilder = new GridBag()
                .setDefaultWeightX(1.0)
                .setDefaultFill(GridBagConstraints.HORIZONTAL)
                .setDefaultInsets(0, 0, 2, 2);
            gridPanel.add(
                new JBLabel(RsBundle.message("label.struct", ((RsNameIdentifierOwner) myElement).getName())),
                gridBuilder.nextLine().next()
            );

            java.util.List<org.rust.lang.core.psi.RsTupleFieldDecl> input = myElement.getTupleFields().getTupleFieldDeclList();
            for (int i = 0; i < input.size(); i++) {
                gridPanel.add(myEditors.get(i), gridBuilder.nextLine().next());
                gridPanel.add(new JBLabel(": " + input.get(i).getText()), gridBuilder.next());
            }
            gridPanel.setBorder(createContentPaneBorder());
            gridPanel.add(new JBLabel("}"), gridBuilder.weighty(1.0).nextLine());

            panel.add(gridPanel, BorderLayout.NORTH);
            panel.add(myCb, BorderLayout.SOUTH);
            return panel;
        }
    }
}
