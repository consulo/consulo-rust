/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.rust.localize.RustLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

public class CargoRedirectInputFragment implements RunConfigurationFragment<CargoCommandConfiguration> {
    @Override
    public String getId() {
        return "cargoRedirectInput";
    }

    @Override
    public LocalizeValue getName() {
        return RustLocalize.runConfigurationFragmentRedirectInput();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.REDIRECT_INPUT;
    }

    @Override
    public boolean isVisibleByDefault(CargoCommandConfiguration target) {
        return target.isRedirectInput();
    }

    @Override
    public void applyHidden(CargoCommandConfiguration target) {
        target.setRedirectInput(false);
    }

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<CargoCommandConfiguration> createEditor(Project project, Disposable parent) {
        FileChooserTextBoxBuilder.Controller redirectInput = FileChooserTextBoxBuilder.create(project)
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleLocalFileDescriptor())
            .uiDisposable(parent)
            .build();

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(RustLocalize.checkboxRedirectInputFrom(), redirectInput.getComponent());
            }

            @RequiredUIAccess
            @Override
            public void reset(CargoCommandConfiguration target) {
                redirectInput.setValue(FileUtil.toSystemDependentName(StringUtil.notNullize(target.getRedirectInputPath())), false);
            }

            @RequiredUIAccess
            @Override
            public void apply(CargoCommandConfiguration target) {
                String path = StringUtil.notNullize(redirectInput.getValue()).trim();
                target.setRedirectInput(true);
                target.setRedirectInputPath(path.isEmpty() ? null : FileUtil.toSystemIndependentName(path));
            }
        };
    }
}
