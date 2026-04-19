/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractStructFields;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsNamesValidator;

import javax.swing.*;

class ExtractFieldsDialog extends DialogWrapper implements ExtractFieldsUi {
    @NotNull
    private final JBTextField myInput;

    ExtractFieldsDialog(@NotNull Project project) {
        super(project, false);
        myInput = new JBTextField();
        init();
        setTitle(RsBundle.message("action.Rust.RsExtractStructFields.choose.name.dialog.title"));
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (!RsNamesValidator.isValidRustVariableIdentifier(myInput.getText())) {
            return new ValidationInfo(
                RsBundle.message("action.Rust.RsExtractStructFields.choose.name.dialog.invalid.name"),
                myInput
            );
        }
        return null;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(myInput);
        return panel;
    }

    @Nullable
    @Override
    public String selectStructName(@NotNull Project project) {
        return showAndGet() ? myInput.getText() : null;
    }
}
