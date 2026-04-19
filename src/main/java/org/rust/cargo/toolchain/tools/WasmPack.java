/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.util.CargoArgsParserUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WasmPack extends CargoBinary {

    public static final String NAME = "wasm-pack";

    public WasmPack(RsToolchainBase toolchain) {
        super(NAME, toolchain);
    }

    public GeneralCommandLine createCommandLine(
        File workingDirectory,
        String command,
        List<String> args,
        boolean emulateTerminal
    ) {
        List<List<String>> split = CargoArgsParserUtil.splitOnDoubleDash(args);
        List<String> pre = new ArrayList<>(split.get(0));
        List<String> post = new ArrayList<>(split.get(1));

        pre.add(0, command);

        Set<String> buildableCommands = Set.of("build", "test");
        String forceColorsOption = "--color=always";
        if (buildableCommands.contains(command) && !post.contains(forceColorsOption)) {
            post.add(forceColorsOption);
        }

        List<String> allArgs;
        if (post.isEmpty()) {
            allArgs = pre;
        } else {
            allArgs = new ArrayList<>(pre);
            allArgs.add("--");
            allArgs.addAll(post);
        }

        GeneralCommandLine commandLine = createBaseCommandLine(allArgs, workingDirectory.toPath())
            .withRedirectErrorStream(true);

        if (emulateTerminal) {
            commandLine = new PtyCommandLine(commandLine)
                .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
                .withConsoleMode(false);
        }

        return commandLine;
    }

    @Nullable
    public static WasmPack create(RsToolchainBase toolchain) {
        return toolchain.hasCargoExecutable(NAME) ? new WasmPack(toolchain) : null;
    }
}
