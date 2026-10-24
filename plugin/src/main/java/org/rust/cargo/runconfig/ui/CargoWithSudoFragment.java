/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.TagRunConfigurationFragment;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.rust.localize.RustLocalize;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

public class CargoWithSudoFragment implements TagRunConfigurationFragment<CargoCommandConfiguration> {
    @Override
    public String getId() {
        return "cargoWithSudo";
    }

    @Override
    public LocalizeValue getName() {
        return Platform.current().os().isWindows()
            ? RustLocalize.checkboxRunWithAdministratorPrivileges()
            : RustLocalize.checkboxRunWithRootPrivileges();
    }

    @Override
    public boolean isApplicable(RunConfiguration configuration) {
        return OpenApiUtil.isFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW);
    }

    @Override
    public boolean isSet(CargoCommandConfiguration target) {
        return target.getWithSudo();
    }

    @Override
    public void set(CargoCommandConfiguration target, boolean value) {
        target.setWithSudo(value);
    }
}
