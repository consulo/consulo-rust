/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractStructFields;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.ui.ex.awt.JBTextField;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.names.RsNamesValidator;

import javax.swing.*;

class ExtractFieldsDialog extends DialogWrapper implements ExtractFieldsUi {
    @Nonnull
    private final JBTextField myInput;

    ExtractFieldsDialog(@Nonnull Project project) {
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
    public String selectStructName(@Nonnull Project project) {
        return showAndGet() ? myInput.getText() : null;
    }
}
