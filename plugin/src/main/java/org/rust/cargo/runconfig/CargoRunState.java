/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.execution.ui.console.Filter;
import consulo.execution.runner.ExecutionEnvironment;
import jakarta.annotation.Nonnull;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.console.RsConsoleBuilder;

public class CargoRunState extends CargoRunStateBase {

    public CargoRunState(
        @Nonnull ExecutionEnvironment environment,
        @Nonnull CargoCommandConfiguration runConfiguration,
        @Nonnull CargoCommandConfiguration.CleanConfiguration.Ok config
    ) {
        super(environment, runConfiguration, config);
        setConsoleBuilder(new RsConsoleBuilder(getProject(), runConfiguration));
        for (Filter filter : RunConfigUtil.createFilters(getCargoProject())) {
            getConsoleBuilder().addFilter(filter);
        }
    }
}
