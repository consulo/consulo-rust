/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.ui.RawCommandLineEditor;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.RustChannel;

import javax.swing.*;
import java.util.Arrays;

public class RustfmtConfigurable extends RsConfigurableBase {

    private final RawCommandLineEditor additionalArguments;
    private final JLabel channelLabel;
    private final ComboBox<RustChannel> channel;
    private final EnvironmentVariablesComponent environmentVariables;

    public RustfmtConfigurable(@NotNull Project project) {
        super(project, RsBundle.message("settings.rust.rustfmt.name"));
        this.additionalArguments = new RawCommandLineEditor();
        this.channelLabel = new JBLabel(RsBundle.message("settings.rust.rustfmt.channel.label"));
        this.channel = new ComboBox<>();
        Arrays.stream(RustChannel.values())
            .sorted((a, b) -> Integer.compare(a.getIndex(), b.getIndex()))
            .forEach(channel::addItem);
        this.environmentVariables = new EnvironmentVariablesComponent();
    }

    @NotNull
    @Override
    public DialogPanel createPanel() {
        return new DialogPanel();
    }
}
