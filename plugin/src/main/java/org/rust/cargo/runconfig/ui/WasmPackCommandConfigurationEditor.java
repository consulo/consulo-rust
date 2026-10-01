/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.execution.localize.ExecutionLocalize;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.rust.localize.RustLocalize;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.util.FormBuilder;
import jakarta.annotation.Nonnull;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.runconfig.wasmpack.WasmPackCommandConfiguration;
import org.rust.cargo.runconfig.wasmpack.util.WasmPackCommandCompletionProvider;

public class WasmPackCommandConfigurationEditor extends RsCommandConfigurationEditor<WasmPackCommandConfiguration> {
    public WasmPackCommandConfigurationEditor(@Nonnull Project project) {
        super(project);
    }

    @Nonnull
    @Override
    protected TextFieldCompletionProvider createCommandCompletionProvider() {
        return new WasmPackCommandCompletionProvider(CargoProjectServiceUtil.getCargoProjects(project), this::currentWorkspace);
    }

    @Override
    @RequiredUIAccess
    protected Component createForm(EditorBox command, CheckBox emulateTerminal, FileChooserTextBoxBuilder.Controller workingDirectory) {
        return FormBuilder.create()
            .addLabeled(RustLocalize.command2(), command)
            .addLabeled(LocalizeValue.empty(), emulateTerminal)
            .addLabeled(ExecutionLocalize.runConfigurationWorkingDirectoryLabel(), workingDirectory.getComponent())
            .build();
    }
}
