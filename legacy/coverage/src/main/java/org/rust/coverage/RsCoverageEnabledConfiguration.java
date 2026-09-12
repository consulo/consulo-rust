/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import com.intellij.coverage.CoverageRunner;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration;
import com.intellij.execution.process.ProcessHandler;
import org.jetbrains.annotations.Nullable;

public class RsCoverageEnabledConfiguration extends CoverageEnabledConfiguration {

    @Nullable
    public ProcessHandler coverageProcess;

    public RsCoverageEnabledConfiguration(RunConfigurationBase<?> configuration) {
        super(configuration);
        coverageProcess = null;
        setCoverageRunner(CoverageRunner.getInstance(RsCoverageRunner.class));
    }
}
