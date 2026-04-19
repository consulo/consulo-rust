/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.RsBundle;
import org.rust.ide.newProject.RsPackageNameValidator;
import org.rust.openapiext.UiUtil;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import org.rust.stdext.BuilderUtil;

public class CargoNewCrateDialog extends DialogWrapper implements CargoNewCrateUI {
    private final VirtualFile root;
    private final ComboBox<String> typeCombobox = new ComboBox<>(new String[]{"Binary", "Library"});
    private final JBTextField name = new JBTextField(20);

    public CargoNewCrateDialog(Project project, VirtualFile root) {
        super(project);
        this.root = root;
        setTitle(RsBundle.message("dialog.title.new.cargo.crate"));
        init();
    }

    public boolean getBinary() {
        return typeCombobox.getSelectedIndex() == 0;
    }

    public String getCrateName() {
        return UiUtil.trimmedText(name);
    }

    @Override
    public CargoNewCrateSettings selectCargoCrateSettings() {
        boolean result = showAndGet();
        if (!result) return null;
        return new CargoNewCrateSettings(getBinary(), getCrateName());
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new java.awt.GridLayout(2, 2, 5, 5));
        panel.add(new JLabel(RsBundle.message("name")));
        panel.add(name);
        panel.add(new JLabel(RsBundle.message("type")));
        panel.add(typeCombobox);
        return panel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return name;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
        String crateName = getCrateName();

        String validationError = RsPackageNameValidator.validate(crateName, getBinary());
        if (validationError != null) {
            return new ValidationInfo(validationError, name);
        }

        if (root.findChild(crateName) != null) {
            return new ValidationInfo(RsBundle.message("dialog.message.directory.already.exists", crateName), name);
        }
        return null;
    }

    private static CargoNewCrateUI MOCK = null;

    public static CargoNewCrateUI showCargoNewCrateUI(Project project, VirtualFile root) {
        if (OpenApiUtil.isUnitTestMode()) {
            if (MOCK == null) throw new IllegalStateException("You should set mock ui via `withMockCargoNewCrateUi`");
            return MOCK;
        } else {
            return new CargoNewCrateDialog(project, root);
        }
    }

    @TestOnly
    public static void withMockCargoNewCrateUi(CargoNewCrateUI mockUi, Runnable action) {
        MOCK = mockUi;
        try {
            action.run();
        } finally {
            MOCK = null;
        }
    }
}
