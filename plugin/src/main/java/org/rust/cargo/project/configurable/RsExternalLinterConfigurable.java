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
import consulo.ui.ex.awt.EnumComboBoxModel;
import consulo.ui.ex.awt.JBLabel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.ExternalLinter;
import org.rust.cargo.toolchain.RustChannel;
import org.rust.cargo.util.CargoCommandCompletionProvider;
import org.rust.cargo.util.RsCommandLineEditor;

import consulo.ui.ex.awt.FormBuilder;
import consulo.ui.ex.awt.UIUtil;
import org.rust.cargo.project.settings.RsExternalLinterProjectSettingsService;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.Objects;

@ExtensionImpl
public class RsExternalLinterConfigurable extends RsConfigurableBase implements ProjectConfigurable {

    private final RsCommandLineEditor additionalArguments;
    private final JLabel channelLabel;
    private final ComboBox<RustChannel> channel;
    private final EnvironmentVariablesComponent environmentVariables;

    @Inject
    public RsExternalLinterConfigurable(@Nonnull Project project) {
        super(project, RsBundle.message("settings.rust.external.linters.name"));
        this.additionalArguments = new RsCommandLineEditor(
            project,
            new CargoCommandCompletionProvider(CargoProjectServiceUtil.getCargoProjects(project), "check ", () -> null)
        );
        this.channelLabel = new JBLabel(RsBundle.message("settings.rust.external.linters.channel.label"));
        this.channel = new ComboBox<>();
        Arrays.stream(RustChannel.values())
            .sorted((a, b) -> Integer.compare(a.getIndex(), b.getIndex()))
            .forEach(channel::addItem);
        this.environmentVariables = new EnvironmentVariablesComponent();
    }

    @Nonnull
    @Override
    public String getId() {
        return "language.rust.cargo.check";
    }

    @Nullable
    @Override
    public String getParentId() {
        return RsProjectConfigurable.ID;
    }

    @Nonnull
    @Override
    public DialogPanel createPanel() {
        RsExternalLinterProjectSettingsService settings = RsProjectSettingsServiceUtil.getExternalLinterSettings(project);

        ComboBox<ExternalLinter> tool = new ComboBox<>(new EnumComboBoxModel<>(ExternalLinter.class));
        JCheckBox runOnTheFly = new JCheckBox(RsBundle.message("settings.rust.external.linters.on.the.fly.label"));

        channelLabel.setLabelFor(channel);
        JPanel argumentsRow = new JPanel(new BorderLayout(UIUtil.DEFAULT_HGAP, 0));
        argumentsRow.add(additionalArguments, BorderLayout.CENTER);
        JPanel channelRow = new JPanel(new BorderLayout(UIUtil.DEFAULT_HGAP, 0));
        channelRow.add(channelLabel, BorderLayout.WEST);
        channelRow.add(channel, BorderLayout.CENTER);
        argumentsRow.add(channelRow, BorderLayout.EAST);

        JPanel form = FormBuilder.createFormBuilder()
            .addLabeledComponent(RsBundle.message("settings.rust.external.linters.tool.label"), tool)
            .addTooltip(RsBundle.message("settings.rust.external.linters.tool.comment"))
            .addLabeledComponent(RsBundle.message("settings.rust.external.linters.additional.arguments.label"), argumentsRow)
            .addTooltip(RsBundle.message("settings.rust.external.linters.additional.arguments.comment"))
            .addComponent(environmentVariables)
            .addComponent(runOnTheFly)
            .addTooltip(RsBundle.message("settings.rust.external.linters.on.the.fly.comment"))
            .getPanel();

        DialogPanel panel = new DialogPanel(new BorderLayout());
        panel.add(form, BorderLayout.NORTH);

        panel.bind(
            () -> tool.setSelectedItem(settings.getTool()),
            () -> settings.modify(state -> state.tool = (ExternalLinter) tool.getSelectedItem()),
            () -> !Objects.equals(tool.getSelectedItem(), settings.getTool()));
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
            () -> runOnTheFly.setSelected(settings.getRunOnTheFly()),
            () -> settings.modify(state -> state.runOnTheFly = runOnTheFly.isSelected()),
            () -> runOnTheFly.isSelected() != settings.getRunOnTheFly());

        return panel;
    }
}
