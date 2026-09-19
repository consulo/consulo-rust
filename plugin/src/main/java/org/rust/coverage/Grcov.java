/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import consulo.process.cmd.GeneralCommandLine;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.CargoBinary;
import org.rust.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Grcov extends CargoBinary {

    public static final String NAME = "grcov";

    public Grcov(@Nonnull RsToolchainBase toolchain) {
        super(NAME, toolchain);
    }

    /**
     * Extension function converted from Kotlin: RsToolchainBase.grcov()
     */
    @Nullable
    public static Grcov grcov(@Nonnull RsToolchainBase toolchain) {
        return toolchain.hasCargoExecutable(NAME) ? new Grcov(toolchain) : null;
    }

    // Parameters are copied from here - https://github.com/mozilla/grcov#grcov-with-travis
    @Nonnull
    public GeneralCommandLine createCommandLine(@Nonnull Path workingDirectory, @Nonnull Path coverageFilePath) {
        List<String> parameters = new ArrayList<>(List.of(
            ".",
            "-s", ".",
            "-t", "lcov",
            "--branch",
            "--ignore-not-existing",
            "--ignore", "/*",
            "-o", coverageFilePath.toString()
        ));
        if (OpenApiUtil.isFeatureEnabled(RsExperiments.SOURCE_BASED_COVERAGE)) {
            parameters.add("--binary-path");
            parameters.add("./target/debug/deps/");
        } else {
            parameters.add("--llvm");
        }
        return createBaseCommandLine(parameters, workingDirectory, Collections.emptyMap());
    }
}
