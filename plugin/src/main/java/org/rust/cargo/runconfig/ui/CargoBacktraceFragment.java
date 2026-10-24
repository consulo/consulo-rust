/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.rust.localize.RustLocalize;
import consulo.ui.ComboBox;
import consulo.ui.annotation.RequiredUIAccess;
import org.rust.cargo.api.toolchain.BacktraceMode;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

public class CargoBacktraceFragment implements RunConfigurationFragment<CargoCommandConfiguration> {
    @Override
    public String getId() {
        return "cargoBacktrace";
    }

    @Override
    public LocalizeValue getName() {
        return RustLocalize.runConfigurationFragmentBacktrace();
    }

    @Override
    public boolean isVisibleByDefault(CargoCommandConfiguration target) {
        return target.getBacktrace() != BacktraceMode.SHORT;
    }

    @Override
    public void applyHidden(CargoCommandConfiguration target) {
        target.setBacktrace(BacktraceMode.SHORT);
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<CargoCommandConfiguration> createEditor(Project project, Disposable parent) {
        ComboBox<BacktraceMode> backtrace = ComboBox.create(BacktraceMode.values());
        backtrace.setTextRenderer(value -> value == null ? LocalizeValue.empty() : LocalizeValue.of(value.toString()));

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(RustLocalize.backtrace(), backtrace);
            }

            @RequiredUIAccess
            @Override
            public void reset(CargoCommandConfiguration target) {
                backtrace.setValue(target.getBacktrace());
            }

            @RequiredUIAccess
            @Override
            public void apply(CargoCommandConfiguration target) {
                target.setBacktrace(backtrace.getValue() == null ? BacktraceMode.SHORT : backtrace.getValue());
            }
        };
    }
}
