/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.rust.localize.RustLocalize;
import consulo.ui.ComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import org.rust.cargo.api.toolchain.RustChannel;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RsToolchainLocator;
import org.rust.cargo.toolchain.tools.Rustup;

public class CargoChannelFragment implements RunConfigurationFragment<CargoCommandConfiguration> {
    @Override
    public String getId() {
        return "cargoChannel";
    }

    @Override
    public LocalizeValue getName() {
        return RustLocalize.runConfigurationFragmentChannel();
    }

    @Override
    public boolean isVisibleByDefault(CargoCommandConfiguration target) {
        return target.getChannel() != RustChannel.DEFAULT;
    }

    @Override
    public void applyHidden(CargoCommandConfiguration target) {
        target.setChannel(RustChannel.DEFAULT);
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<CargoCommandConfiguration> createEditor(Project project, Disposable parent) {
        ComboBox<RustChannel> channel = ComboBox.create(RustChannel.values());
        channel.setTextRenderer(value -> value == null ? LocalizeValue.empty() : LocalizeValue.of(value.toString()));

        return new RunConfigurationFragmentEditor<>() {
            private boolean myRustupAvailable = true;

            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(RustLocalize.labelChannel(), channel);
            }

            @RequiredUIAccess
            @Override
            public void reset(CargoCommandConfiguration target) {
                RsToolchainBase toolchain = RsToolchainLocator.getToolchain(project);
                myRustupAvailable = toolchain != null && Rustup.isRustupAvailable(toolchain);

                channel.setValue(target.getChannel());
                channel.setEnabled(myRustupAvailable || target.getChannel() != RustChannel.DEFAULT);
            }

            @RequiredUIAccess
            @Override
            public void apply(CargoCommandConfiguration target) throws ConfigurationException {
                RustChannel selected = channel.getValue() == null ? RustChannel.DEFAULT : channel.getValue();
                target.setChannel(selected);
                if (!myRustupAvailable && selected != RustChannel.DEFAULT) {
                    throw new ConfigurationException(RustLocalize.dialogMessageChannelCannotBeSetExplicitlyBecauseRustupNotAvailable());
                }
            }
        };
    }
}
