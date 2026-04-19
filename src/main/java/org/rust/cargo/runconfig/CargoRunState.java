/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.console.RsConsoleBuilder;

public class CargoRunState extends CargoRunStateBase {

    public CargoRunState(
        @NotNull ExecutionEnvironment environment,
        @NotNull CargoCommandConfiguration runConfiguration,
        @NotNull CargoCommandConfiguration.CleanConfiguration.Ok config
    ) {
        super(environment, runConfiguration, config);
        setConsoleBuilder(new RsConsoleBuilder(getProject(), runConfiguration));
        for (Filter filter : RunConfigUtil.createFilters(getCargoProject())) {
            getConsoleBuilder().addFilter(filter);
        }
    }
}
