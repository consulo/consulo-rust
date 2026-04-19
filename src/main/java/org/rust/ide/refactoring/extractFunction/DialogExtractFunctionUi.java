/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.refactoring.ui.MethodSignatureComponent;
import com.intellij.refactoring.ui.NameSuggestionsField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsNamesValidator;
import org.rust.lang.RsFileType;

import javax.swing.*;
import java.util.Collections;

class DialogExtractFunctionUi implements ExtractFunctionUi {
    @NotNull
    private final Project myProject;

    DialogExtractFunctionUi(@NotNull Project project) {
        myProject = project;
    }

    @Override
    public void extract(@NotNull RsExtractFunctionConfig config, @NotNull Runnable callback) {
        NameSuggestionsField functionNameField = new NameSuggestionsField(new String[0], myProject, RsFileType.INSTANCE);
        functionNameField.setMinimumSize(JBUI.size(300, 30));

        ComboBox<String> visibilityBox = new ComboBox<>();
        visibilityBox.addItem(RsBundle.message("public"));
        visibilityBox.addItem(RsBundle.message("private"));
        visibilityBox.setSelectedItem(RsBundle.message("private"));

        MethodSignatureComponent signatureComponent = new MethodSignatureComponent(
            config.getSignature(), myProject, RsFileType.INSTANCE
        ) {
            @Override
            protected String getFileName() {
                return "dummy." + RsFileType.INSTANCE.getDefaultExtension();
            }
        };
        signatureComponent.setMinimumSize(JBUI.size(300, 30));

        visibilityBox.addActionListener(e -> {
            updateConfig(config, functionNameField, visibilityBox);
            signatureComponent.setSignature(config.getSignature());
        });

        // Create a simple dialog
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(functionNameField);
        panel.add(visibilityBox);
        panel.add(signatureComponent);

        DialogWrapper dialog = new DialogWrapper(myProject) {
            {
                init();
                setTitle(RsBundle.message("dialog.title.extract.function"));
            }

            @Override
            protected JComponent createCenterPanel() {
                return panel;
            }

            @Override
            protected void doOKAction() {
                updateConfig(config, functionNameField, visibilityBox);
                callback.run();
                super.doOKAction();
            }
        };

        functionNameField.addDataChangedListener(() -> {
            updateConfig(config, functionNameField, visibilityBox);
            signatureComponent.setSignature(config.getSignature());
            dialog.setOKActionEnabled(RsNamesValidator.isValidRustVariableIdentifier(config.getName()));
        });

        dialog.show();
    }

    private void updateConfig(
        @NotNull RsExtractFunctionConfig config,
        @NotNull NameSuggestionsField functionName,
        @NotNull ComboBox<String> visibilityBox
    ) {
        config.setName(functionName.getEnteredName());
        config.setVisibilityLevelPublic("Public".equals(visibilityBox.getSelectedItem()));
    }
}
