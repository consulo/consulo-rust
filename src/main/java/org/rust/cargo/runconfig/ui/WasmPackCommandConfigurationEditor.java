/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.runconfig.wasmpack.WasmPackCommandConfiguration;
import org.rust.cargo.runconfig.wasmpack.util.WasmPackCommandCompletionProvider;
import org.rust.cargo.util.RsCommandLineEditor;
import org.rust.openapiext.UiDslUtil;

import javax.swing.*;

public class WasmPackCommandConfigurationEditor extends RsCommandConfigurationEditor<WasmPackCommandConfiguration> {

    @NotNull
    private final RsCommandLineEditor command;

    public WasmPackCommandConfigurationEditor(@NotNull Project project) {
        super(project);
        this.command = new RsCommandLineEditor(
            project, new WasmPackCommandCompletionProvider(CargoProjectServiceUtil.getCargoProjects(project), () -> currentWorkspace())
        );
    }

    @NotNull
    @Override
    public RsCommandLineEditor getCommand() {
        return command;
    }

    @NotNull
    @Override
    protected JComponent createEditor() {
        com.intellij.util.ui.FormBuilder builder = com.intellij.util.ui.FormBuilder.createFormBuilder();
        builder.addLabeledComponent(RsBundle.message("command2"), command);
        builder.addComponent(emulateTerminal);
        builder.addLabeledComponent(workingDirectory.getLabel(), workingDirectory.getComponent());
        return builder.getPanel();
    }
}
