/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import java.util.*;
import java.util.regex.Pattern;

public class CargoArgsParser {

    private final List<String> cargoArgs;
    private final Map<String, OptionArgsCountRange> optionsToArgsCountRange;

    private CargoArgsParser(List<String> cargoArgs, Map<String, OptionArgsCountRange> optionsToArgsCountRange) {
        this.cargoArgs = cargoArgs;
        this.optionsToArgsCountRange = optionsToArgsCountRange;
    }

    private SplitCargoArgs splitArgs() {
        List<String> commandOptions = new ArrayList<>();
        List<String> positionalArguments = new ArrayList<>();

        int i = 0;
        while (i < cargoArgs.size()) {
            String arg = cargoArgs.get(i);
            if ("--".equals(arg)) {
                // End of options - collect remaining arguments as positional-only arguments
                positionalArguments.addAll(cargoArgs.subList(i, cargoArgs.size()));
                break;
            } else if (arg.startsWith("-")) {
                // An option
                int optionArgsCount = getActualOptionArgsCount(i);
                List<String> optionWithArgs = cargoArgs.subList(i, i + optionArgsCount + 1);
                commandOptions.addAll(optionWithArgs);
                i += optionWithArgs.size();
            } else {
                // A positional arg
                positionalArguments.add(arg);
                i += 1;
            }
        }

        return new SplitCargoArgs(commandOptions, positionalArguments);
    }

    private int getActualOptionArgsCount(int optionIdx) {
        String option = cargoArgs.get(optionIdx);
        // Unknown options are assumed to be the flagging options
        OptionArgsCountRange range = optionsToArgsCountRange.getOrDefault(option, OptionArgsCountRange.ZERO);
        int maxArgsCount = range.max();
        int count = 0;
        for (int j = optionIdx + 1; j < cargoArgs.size() && count < maxArgsCount; j++) {
            String s = cargoArgs.get(j);
            if ("--".equals(s) || OPTION_NAME_RE.matcher(s).matches()) {
                break;
            }
            count++;
        }
        return count;
    }

    private static final String OPTION_CHAR_CLASS = "[a-zA-Z0-9]";
    private static final Pattern OPTION_NAME_RE =
        Pattern.compile("^(-" + OPTION_CHAR_CLASS + ")|(--" + OPTION_CHAR_CLASS + "+([-_.]" + OPTION_CHAR_CLASS + "+)*)$");

    private static final Map<String, OptionArgsCountRange> RUN_OPTIONS = new HashMap<>(Map.ofEntries(
        Map.entry("--bin", OptionArgsCountRange.ONE),
        Map.entry("--example", OptionArgsCountRange.ONE),
        Map.entry("-p", OptionArgsCountRange.ONE),
        Map.entry("--package", OptionArgsCountRange.ONE),
        Map.entry("-j", OptionArgsCountRange.ONE),
        Map.entry("--jobs", OptionArgsCountRange.ONE),
        Map.entry("--color", OptionArgsCountRange.ONE),
        Map.entry("--profile", OptionArgsCountRange.ONE),
        Map.entry("-F", OptionArgsCountRange.MANY),
        Map.entry("--features", OptionArgsCountRange.MANY),
        Map.entry("--config", OptionArgsCountRange.ONE),
        Map.entry("-Z", OptionArgsCountRange.ONE),
        Map.entry("--target", OptionArgsCountRange.ONE),
        Map.entry("--target-dir", OptionArgsCountRange.ONE),
        Map.entry("--manifest-path", OptionArgsCountRange.ONE),
        Map.entry("--message-format", OptionArgsCountRange.ONE)
    ));

    private static final Map<String, OptionArgsCountRange> TEST_OPTIONS;
    static {
        TEST_OPTIONS = new HashMap<>(RUN_OPTIONS);
        TEST_OPTIONS.put("--test", OptionArgsCountRange.ONE);
        TEST_OPTIONS.put("--bench", OptionArgsCountRange.ONE);
        TEST_OPTIONS.put("--exclude", OptionArgsCountRange.ONE);
    }

    public static ParsedCargoArgs parseArgs(String commandName, List<String> cargoArgs) {
        return switch (commandName) {
            case "run" -> parseRunArgs(cargoArgs);
            case "test", "bench" -> parseTestArgs(cargoArgs);
            default -> throw new IllegalStateException("Unsupported command");
        };
    }

    private static ParsedCargoArgs parseRunArgs(List<String> cargoArgs) {
        CargoArgsParser argsParser = new CargoArgsParser(cargoArgs, RUN_OPTIONS);
        SplitCargoArgs split = argsParser.splitArgs();
        List<String> executableArguments;
        if (!split.positionalArguments().isEmpty() && "--".equals(split.positionalArguments().get(0))) {
            executableArguments = split.positionalArguments().subList(1, split.positionalArguments().size());
        } else {
            executableArguments = split.positionalArguments();
        }
        return new ParsedCargoArgs(split.commandOptions(), executableArguments);
    }

    private static ParsedCargoArgs parseTestArgs(List<String> cargoArgs) {
        CargoArgsParser argsParser = new CargoArgsParser(cargoArgs, TEST_OPTIONS);
        SplitCargoArgs split = argsParser.splitArgs();
        List<List<String>> splitResult = CargoArgsParserUtil.splitOnDoubleDash(split.positionalArguments());
        List<String> positionalPre = splitResult.get(0);
        List<String> positionalPost = splitResult.get(1);

        // Don't drop the last element of the `positionalPre` so that Cargo will check the arguments
        List<String> commandArguments = new ArrayList<>(split.commandOptions());
        commandArguments.addAll(positionalPre);

        // The last positional argument before `--` and all arguments after are passed to the test binary
        List<String> executableArguments = new ArrayList<>();
        if (!positionalPre.isEmpty()) {
            executableArguments.add(positionalPre.get(positionalPre.size() - 1));
        }
        executableArguments.addAll(positionalPost);

        return new ParsedCargoArgs(commandArguments, executableArguments);
    }

    private record OptionArgsCountRange(int min, int max) {
        static final OptionArgsCountRange ZERO = new OptionArgsCountRange(0, 0);
        static final OptionArgsCountRange ONE = new OptionArgsCountRange(1, 1);
        static final OptionArgsCountRange MANY = new OptionArgsCountRange(1, Integer.MAX_VALUE);
    }

    private record SplitCargoArgs(List<String> commandOptions, List<String> positionalArguments) {
    }
}
