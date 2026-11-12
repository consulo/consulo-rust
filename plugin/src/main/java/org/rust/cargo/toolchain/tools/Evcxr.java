/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import java.nio.file.Path;
import com.intellij.execution.configurations.PtyCommandLine;
import jakarta.annotation.Nullable;
import org.rust.cargo.toolchain.RsToolchainBase;


public class Evcxr extends CargoBinary {

    public static final String NAME = "evcxr";

    public Evcxr(RsToolchainBase toolchain) {
        super(NAME, toolchain);
    }

    public PtyCommandLine createCommandLine(Path workingDirectory) {
        var commandLine = createBaseCommandLine(
            new String[]{"--ide-mode", "--disable-readline", "--opt", "0"},
            workingDirectory,
            java.util.Collections.emptyMap()
        );
        return new PtyCommandLine(commandLine).withInitialColumns(PtyCommandLine.MAX_COLUMNS);
    }

    @Nullable
    public static Evcxr create(RsToolchainBase toolchain) {
        return toolchain.hasCargoExecutable(NAME) ? new Evcxr(toolchain) : null;
    }
}
