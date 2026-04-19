/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractTrait;

import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsMemberInfo;
import org.rust.ide.refactoring.RsMemberSelectionPanel;
import org.rust.ide.refactoring.RsNamesValidator;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.List;
import java.util.stream.Collectors;

class RsExtractTraitDialog extends RefactoringDialog {
    private static final Logger LOG = Logger.getInstance(RsExtractTraitDialog.class);

    @NotNull
    private final RsTraitOrImpl myTraitOrImpl;
    @NotNull
    private final List<RsMemberInfo> myMemberInfos;
    @NotNull
    private final JTextField myTraitNameField;

    RsExtractTraitDialog(
        @NotNull Project project,
        @NotNull RsTraitOrImpl traitOrImpl,
        @NotNull List<RsMemberInfo> memberInfos
    ) {
        super(project, false);
        myTraitOrImpl = traitOrImpl;
        myMemberInfos = memberInfos;
        myTraitNameField = new JBTextField();
        myTraitNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateButtons();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateButtons();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateButtons();
            }
        });
        init();
        setTitle(RsBundle.message("action.Rust.RsExtractTrait.dialog.title"));
        validateButtons();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel label = new JLabel(RsBundle.message("label.trait.name"));
        panel.add(label);
        panel.add(myTraitNameField);

        RsMemberSelectionPanel membersPanel = new RsMemberSelectionPanel(
            RsBundle.message("separator.members.to.form.trait"),
            myMemberInfos
        );
        membersPanel.setMinimumSize(JBUI.size(0, 200));
        membersPanel.getTable().addMemberInfoChangeListener(l -> validateButtons());
        panel.add(membersPanel);

        return panel;
    }

    @Override
    protected void validateButtons() {
        super.validateButtons();
        getPreviewAction().setEnabled(false);
    }

    @Override
    protected boolean areButtonsValid() {
        boolean hasChecked = false;
        for (RsMemberInfo info : myMemberInfos) {
            if (info.isChecked()) {
                hasChecked = true;
                break;
            }
        }
        return RsNamesValidator.isValidRustVariableIdentifier(myTraitNameField.getText()) && hasChecked;
    }

    @Override
    protected void doAction() {
        try {
            CommandProcessor.getInstance().executeCommand(
                myProject,
                this::doActionUndoCommand,
                getTitle(),
                null
            );
        } catch (Exception e) {
            LOG.error(e);
            String title = RefactoringBundle.message("error.title");
            CommonRefactoringUtil.showErrorMessage(title, e.getMessage(), null, myProject);
        }
    }

    private void doActionUndoCommand() {
        List<RsItemElement> selectedMembers = myMemberInfos.stream()
            .filter(RsMemberInfo::isChecked)
            .map(RsMemberInfo::getMember)
            .collect(Collectors.toList());
        String traitName = myTraitNameField.getText();
        RsExtractTraitProcessor processor = new RsExtractTraitProcessor(myTraitOrImpl, traitName, selectedMembers);
        invokeRefactoring(processor);
    }
}
