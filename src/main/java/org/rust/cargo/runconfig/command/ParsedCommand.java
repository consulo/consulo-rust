/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ParsedCommand(String command, @Nullable String toolchain, List<String> additionalArguments) {
    @Nullable
    public static ParsedCommand parse(String rawCommand) {
        List<String> args = ParametersListUtil.parse(rawCommand);
        String command = null;
        for (String arg : args) {
            if (!arg.startsWith("+")) {
                command = arg;
                break;
            }
        }
        if (command == null) return null;
        String first = args.isEmpty() ? null : args.get(0);
        String toolchain = (first != null && first.startsWith("+")) ? first.substring(1) : null;
        int commandIdx = args.indexOf(command);
        List<String> additionalArguments = args.subList(commandIdx + 1, args.size());
        return new ParsedCommand(command, toolchain, additionalArguments);
    }
}
