/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.ProjectConfigurable;
import consulo.execution.ui.awt.EnvironmentVariablesComponent;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import com.intellij.openapi.ui.DialogPanel;
import consulo.execution.ui.awt.RawCommandLineEditor;
import consulo.ui.ex.awt.JBLabel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.rust.RsBundle;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.api.toolchain.RustChannel;

import consulo.ui.ex.awt.FormBuilder;
import consulo.ui.ex.awt.UIUtil;
import org.rust.cargo.api.settings.RustfmtProjectSettingsService;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Objects;

@ExtensionImpl
public class RustfmtConfigurable extends RsConfigurableBase implements ProjectConfigurable {

    private final RawCommandLineEditor additionalArguments;
    private final JLabel channelLabel;
    private final ComboBox<RustChannel> channel;
    private final EnvironmentVariablesComponent environmentVariables;

    @Inject
    public RustfmtConfigurable(@Nonnull Project project) {
        super(project, RsBundle.message("settings.rust.rustfmt.name"));
        this.additionalArguments = new RawCommandLineEditor();
        this.channelLabel = new JBLabel(RsBundle.message("settings.rust.rustfmt.channel.label"));
        this.channel = new ComboBox<>();
        Arrays.stream(RustChannel.values())
            .sorted((a, b) -> Integer.compare(a.getIndex(), b.getIndex()))
            .forEach(channel::addItem);
        this.environmentVariables = new EnvironmentVariablesComponent();
    }

    @Nonnull
    @Override
    public String getId() {
        return "language.rust.rustfmt";
    }

    @Nullable
    @Override
    public String getParentId() {
        return RsProjectConfigurable.ID;
    }

    @Nonnull
    @Override
    public DialogPanel createPanel() {
        RustfmtProjectSettingsService settings = RsProjectSettingsServiceUtil.getRustfmtSettings(project);

        JCheckBox useRustfmt = new JCheckBox(RsBundle.message("settings.rust.rustfmt.builtin.formatter.label"));
        JCheckBox runOnSave = new JCheckBox(RsBundle.message("settings.rust.rustfmt.run.on.save.label"));

        channelLabel.setLabelFor(channel);
        JPanel argumentsRow = new JPanel(new BorderLayout(UIUtil.DEFAULT_HGAP, 0));
        argumentsRow.add(additionalArguments, BorderLayout.CENTER);
        JPanel channelRow = new JPanel(new BorderLayout(UIUtil.DEFAULT_HGAP, 0));
        channelRow.add(channelLabel, BorderLayout.WEST);
        channelRow.add(channel, BorderLayout.CENTER);
        argumentsRow.add(channelRow, BorderLayout.EAST);

        JPanel form = FormBuilder.createFormBuilder()
            .addLabeledComponent(RsBundle.message("settings.rust.rustfmt.additional.arguments.label"), argumentsRow)
            .addTooltip(RsBundle.message("settings.rust.rustfmt.additional.arguments.comment"))
            .addComponent(environmentVariables)
            .addComponent(useRustfmt)
            .addComponent(runOnSave)
            .getPanel();

        DialogPanel panel = new DialogPanel(new BorderLayout());
        panel.add(form, BorderLayout.NORTH);

        panel.bind(
            () -> additionalArguments.setText(settings.getAdditionalArguments()),
            () -> settings.modify(state -> state.additionalArguments = additionalArguments.getText()),
            () -> !Objects.equals(additionalArguments.getText(), settings.getAdditionalArguments()));
        panel.bind(
            () -> channel.setSelectedItem(settings.getChannel()),
            () -> settings.modify(state -> state.channel = (RustChannel) channel.getSelectedItem()),
            () -> !Objects.equals(channel.getSelectedItem(), settings.getChannel()));
        panel.bind(
            () -> environmentVariables.setEnvs(settings.getEnvs()),
            () -> settings.modify(state -> state.envs = environmentVariables.getEnvs()),
            () -> !Objects.equals(environmentVariables.getEnvs(), settings.getEnvs()));
        panel.bind(
            () -> useRustfmt.setSelected(settings.getUseRustfmt()),
            () -> settings.modify(state -> state.useRustfmt = useRustfmt.isSelected()),
            () -> useRustfmt.isSelected() != settings.getUseRustfmt());
        panel.bind(
            () -> runOnSave.setSelected(settings.getRunRustfmtOnSave()),
            () -> settings.modify(state -> state.runRustfmtOnSave = runOnSave.isSelected()),
            () -> runOnSave.isSelected() != settings.getRunRustfmtOnSave());

        return panel;
    }
}
