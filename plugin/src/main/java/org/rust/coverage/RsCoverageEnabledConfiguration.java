/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import consulo.execution.coverage.CoverageRunner;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.coverage.CoverageEnabledConfiguration;
import consulo.process.ProcessHandler;
import jakarta.annotation.Nullable;

public class RsCoverageEnabledConfiguration extends CoverageEnabledConfiguration {

    @Nullable
    public ProcessHandler coverageProcess;

    public RsCoverageEnabledConfiguration(RunConfigurationBase configuration) {
        super(configuration);
        coverageProcess = null;
        setCoverageRunner(CoverageRunner.getInstance(RsCoverageRunner.class));
    }
}
