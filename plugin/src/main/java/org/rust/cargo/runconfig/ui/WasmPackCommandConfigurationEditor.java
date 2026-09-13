/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;
import consulo.ui.ex.awt.FormBuilder;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.runconfig.wasmpack.WasmPackCommandConfiguration;
import org.rust.cargo.runconfig.wasmpack.util.WasmPackCommandCompletionProvider;
import org.rust.cargo.util.RsCommandLineEditor;
import org.rust.openapiext.ui.UiDslUtil;

import javax.swing.*;

public class WasmPackCommandConfigurationEditor extends RsCommandConfigurationEditor<WasmPackCommandConfiguration> {

    @Nonnull
    private final RsCommandLineEditor command;

    public WasmPackCommandConfigurationEditor(@Nonnull Project project) {
        super(project);
        this.command = new RsCommandLineEditor(
            project, new WasmPackCommandCompletionProvider(CargoProjectServiceUtil.getCargoProjects(project), () -> currentWorkspace())
        );
    }

    @Nonnull
    @Override
    public RsCommandLineEditor getCommand() {
        return command;
    }

    @Nonnull
    @Override
    protected JComponent createEditor() {
        consulo.ui.ex.awt.FormBuilder builder = consulo.ui.ex.awt.FormBuilder.createFormBuilder();
        builder.addLabeledComponent(RsBundle.message("command2"), command);
        builder.addComponent(emulateTerminal);
        builder.addLabeledComponent(workingDirectory.getLabel(), workingDirectory.getComponent());
        return builder.getPanel();
    }
}
