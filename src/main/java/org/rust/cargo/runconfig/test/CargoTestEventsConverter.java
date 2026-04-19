/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.ServiceMessageBuilder;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.text.SemVer;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.impl.CargoProjectsServiceImpl;
import org.rust.cargo.project.model.impl.CargoProjectsServiceImplUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.util.ToolchainUtil;
import org.rust.openapiext.JsonUtils;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CargoTestEventsConverter extends OutputToGeneralTestEventsConverter {

    private static final SemVer RUSTC_1_73_BETA = ToolchainUtil.parseSemVer("1.73.0-beta");

    private static final String TARGET_PATH_PART = "/target/";
    private static final String ROOT_SUITE = "0";
    private static final String NAME_SEPARATOR = "::";
    private static final String DOCTESTS_SUFFIX = "doctests";

    private static final Pattern NAME_RE = Pattern.compile("\"name\":\\s*\"(?<name>[^\"]+)\"");
    private static final Pattern LINE_NUMBER_RE = Pattern.compile("\\s+\\(line\\s+(?<line>\\d+)\\)\\s*");

    private static final Pattern ERROR_MESSAGE_RE_OLD = Pattern.compile(
        "thread '.*' panicked at '(?<fullMessage>(assertion failed: `\\(left (?<sign>.*) right\\)`\\s*left: `(?<left>.*?)`,\\s*right: `(?<right>.*?)`(: )?)?(?<message>.*))',",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    private static final Pattern ERROR_MESSAGE_RE = Pattern.compile(
        "thread '.*' panicked at (\\S*)\\n(?<fullMessage>(assertion `left (?<sign>.*) right` failed(: )?)?(?<message>.*?)\\n\\s*(left: (?<left>.*?)\\n\\s*right: (?<right>.*?)\\n)?)(note|stack backtrace):",
        Pattern.MULTILINE | Pattern.DOTALL
    );

    private final Project project;
    @Nullable
    private final SemVer rustcVersion;
    private State converterState = State.START_MESSAGE;

    private final List<String> suitesStack = new ArrayList<>();
    private final Map<String, Set<String>> suitesToNotFinishedChildren = new HashMap<>();
    private final Map<String, Long> testsStartTimes = new HashMap<>();
    private final LinkedHashSet<String> pendingFinishedSuites = new LinkedHashSet<>();

    private int doctestPackageCounter = 0;

    public CargoTestEventsConverter(
        @NotNull String testFrameworkName,
        @NotNull TestConsoleProperties consoleProperties,
        @Nullable SemVer rustcVersion
    ) {
        super(testFrameworkName, consoleProperties);
        this.project = consoleProperties.getProject();
        this.rustcVersion = rustcVersion;
    }

    @NotNull
    private String getTarget() {
        return suitesStack.isEmpty() ? ROOT_SUITE : suitesStack.get(0);
    }

    @NotNull
    private String getCurrentSuite() {
        return suitesStack.isEmpty() ? ROOT_SUITE : suitesStack.get(suitesStack.size() - 1);
    }

    @Override
    public boolean processServiceMessages(@NotNull String text, @NotNull Key<?> outputType, @NotNull ServiceMessageVisitor visitor) throws java.text.ParseException {
        handleStartMessage(text);

        Matcher nameMatcher = NAME_RE.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (nameMatcher.find()) {
            String matched = nameMatcher.group();
            String replaced = matched.replace("\\", "\\\\");
            nameMatcher.appendReplacement(sb, Matcher.quoteReplacement(replaced));
        }
        nameMatcher.appendTail(sb);
        String escapedText = sb.toString();

        JsonObject jsonObject = JsonUtils.tryParseJsonObject(escapedText);

        if (jsonObject == null) return false;
        if (handleTestMessage(jsonObject, outputType, visitor)) return true;
        if (handleSuiteMessage(jsonObject, outputType, visitor)) return true;
        return true; // don't print unknown json messages
    }

    private void handleStartMessage(@NotNull String text) {
        switch (converterState) {
            case START_MESSAGE: {
                String clean = text.trim().toLowerCase();
                if ("running".equals(clean)) {
                    converterState = State.EXECUTABLE_NAME;
                } else if ("doc-tests".equals(clean)) {
                    converterState = State.DOCTESTS_PACKAGE_NAME;
                } else if (text.contains(TARGET_PATH_PART)) {
                    List<String> parsed = ParametersListUtil.parse(text);
                    String first = parsed.isEmpty() ? null : parsed.get(0);
                    String executableName = null;
                    if (first != null) {
                        String afterSlash = first.substring(first.lastIndexOf('/') + 1);
                        int dotIdx = afterSlash.lastIndexOf('.');
                        if (dotIdx >= 0) {
                            afterSlash = afterSlash.substring(0, dotIdx);
                        }
                        if (!afterSlash.isEmpty()) {
                            executableName = afterSlash;
                        }
                    }
                    if (executableName == null) {
                        throw new IllegalStateException("Can't parse the executable name");
                    }
                    suitesStack.add(executableName);
                    converterState = State.START_MESSAGE;
                }
                break;
            }
            case EXECUTABLE_NAME: {
                String trimmed = text.trim();
                String fileSeparator = null;
                var toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
                if (toolchain != null) {
                    fileSeparator = toolchain.getFileSeparator();
                }
                if (fileSeparator == null) {
                    fileSeparator = File.separator;
                }
                int sepIdx = trimmed.lastIndexOf(fileSeparator);
                String afterSep = sepIdx >= 0 ? trimmed.substring(sepIdx + fileSeparator.length()) : trimmed;
                int dotIdx = afterSep.lastIndexOf('.');
                if (dotIdx >= 0) {
                    afterSep = afterSep.substring(0, dotIdx);
                }
                if (afterSep.endsWith(")")) {
                    afterSep = afterSep.substring(0, afterSep.length() - 1);
                }
                if (afterSep.isEmpty()) {
                    throw new IllegalStateException("Can't parse the executable name");
                }
                suitesStack.add(afterSep);
                converterState = State.START_MESSAGE;
                break;
            }
            case DOCTESTS_PACKAGE_NAME: {
                String packageName = text.trim();
                CargoWorkspace.Target libTarget = findLibTargetForPackageName(project, packageName);
                String libTargetName = libTarget != null ? libTarget.getNormName() : null;
                String baseName = libTargetName != null ? libTargetName : packageName;
                String targetNameWithSuffix = baseName + "-" + (doctestPackageCounter++) + DOCTESTS_SUFFIX;
                suitesStack.add(targetNameWithSuffix);
                converterState = State.START_MESSAGE;
                break;
            }
        }
    }

    private boolean handleTestMessage(
        @NotNull JsonObject jsonObject,
        @NotNull Key<?> outputType,
        @NotNull ServiceMessageVisitor visitor
    ) throws java.text.ParseException {
        LibtestTestMessage testMessage = LibtestTestMessage.fromJson(jsonObject);
        LibtestBenchMessage benchMessage = testMessage == null ? LibtestBenchMessage.fromJson(jsonObject) : null;

        List<ServiceMessageBuilder> serviceMessages;
        if (testMessage != null) {
            String qualifiedName = testMessage.name.contains(" - ")
                ? testMessage.name.substring(testMessage.name.indexOf(" - ") + 3)
                : testMessage.name;
            LibtestTestMessage fixedMessage = new LibtestTestMessage(
                testMessage.type, testMessage.event, getTarget() + "::" + qualifiedName, testMessage.stdout
            );
            serviceMessages = createServiceMessagesForTest(fixedMessage);
            if (serviceMessages == null) return false;
        } else if (benchMessage != null) {
            LibtestBenchMessage fixedMessage = new LibtestBenchMessage(
                benchMessage.type, getTarget() + "::" + benchMessage.name, benchMessage.median, benchMessage.deviation
            );
            serviceMessages = createServiceMessagesForBench(fixedMessage);
        } else {
            return false;
        }

        for (ServiceMessageBuilder message : serviceMessages) {
            super.processServiceMessages(message.toString(), outputType, visitor);
        }
        return true;
    }

    private boolean handleSuiteMessage(
        @NotNull JsonObject jsonObject,
        @NotNull Key<?> outputType,
        @NotNull ServiceMessageVisitor visitor
    ) throws java.text.ParseException {
        LibtestSuiteMessage suiteMessage = LibtestSuiteMessage.fromJson(jsonObject);
        if (suiteMessage == null) return false;
        List<ServiceMessageBuilder> messages = createServiceMessagesForSuite(suiteMessage);
        if (messages == null) return false;
        for (ServiceMessageBuilder message : messages) {
            super.processServiceMessages(message.toString(), outputType, visitor);
        }
        return true;
    }

    @Nullable
    private List<ServiceMessageBuilder> createServiceMessagesForTest(@NotNull LibtestTestMessage testMessage) {
        List<ServiceMessageBuilder> messages = new ArrayList<>();
        switch (testMessage.event) {
            case "started":
                recordTestStartTime(testMessage.name);
                recursivelyInitContainingSuite(testMessage.name, messages);
                recordSuiteChildStarted(testMessage.name);
                messages.add(createTestStartedMessage(testMessage.name));
                break;
            case "ok": {
                String duration = getTestDuration(testMessage.name);
                if (testMessage.stdout != null && !testMessage.stdout.isEmpty()) {
                    messages.add(createTestStdOutMessage(testMessage.name, testMessage.stdout));
                }
                messages.add(createTestFinishedMessage(testMessage.name, duration));
                recordSuiteChildFinished(testMessage.name);
                processFinishedSuites(messages);
                break;
            }
            case "failed": {
                String duration = getTestDuration(testMessage.name);
                FailedTestOutput output = parseFailedTestOutput(testMessage.stdout != null ? testMessage.stdout : "");
                if (!output.stdout.isEmpty()) {
                    messages.add(createTestStdOutMessage(testMessage.name, output.stdout + "\n"));
                }
                messages.add(createTestFailedMessage(testMessage.name, output.failedMessage, rustcVersion));
                messages.add(createTestFinishedMessage(testMessage.name, duration));
                recordSuiteChildFinished(testMessage.name);
                processFinishedSuites(messages);
                break;
            }
            case "ignored":
                messages.add(createTestIgnoredMessage(testMessage.name));
                break;
            default:
                return null;
        }
        return messages;
    }

    @NotNull
    private List<ServiceMessageBuilder> createServiceMessagesForBench(@NotNull LibtestBenchMessage benchMessage) {
        List<ServiceMessageBuilder> messages = new ArrayList<>();
        String result = benchMessage.median + " ns/iter (+/- " + benchMessage.deviation + ")\n";
        messages.add(createTestStdOutMessage(benchMessage.name, result));
        String duration = getTestDuration(benchMessage.name);
        messages.add(createTestFinishedMessage(benchMessage.name, duration));
        recordSuiteChildFinished(benchMessage.name);
        processFinishedSuites(messages);
        return messages;
    }

    @Nullable
    private List<ServiceMessageBuilder> createServiceMessagesForSuite(@NotNull LibtestSuiteMessage suiteMessage) {
        List<ServiceMessageBuilder> messages = new ArrayList<>();
        switch (suiteMessage.event) {
            case "started":
                getProcessor().onTestsReporterAttached();
                if (Integer.parseInt(suiteMessage.testCount) == 0) return Collections.emptyList();
                messages.add(createTestSuiteStartedMessage(getTarget()));
                messages.add(createTestCountMessage(suiteMessage.testCount));
                break;
            case "ok":
            case "failed":
                for (String suite : pendingFinishedSuites) {
                    messages.add(createTestSuiteFinishedMessage(suite));
                }
                pendingFinishedSuites.clear();
                List<String> reversed = new ArrayList<>(suitesStack);
                Collections.reverse(reversed);
                for (String suite : reversed) {
                    messages.add(createTestSuiteFinishedMessage(suite));
                }
                suitesStack.clear();
                testsStartTimes.clear();
                break;
            default:
                return null;
        }
        return messages;
    }

    private void recordTestStartTime(@NotNull String test) {
        testsStartTimes.put(test, System.currentTimeMillis());
    }

    private void recordSuiteChildStarted(@NotNull String node) {
        String parent = getParent(node);
        suitesToNotFinishedChildren.computeIfAbsent(parent, k -> new HashSet<>()).add(node);
    }

    private void recordSuiteChildFinished(@NotNull String node) {
        String parent = getParent(node);
        Set<String> children = suitesToNotFinishedChildren.get(parent);
        if (children != null) {
            children.remove(node);
        }
    }

    @NotNull
    private String getTestDuration(@NotNull String test) {
        Long startTime = testsStartTimes.get(test);
        if (startTime == null) return ROOT_SUITE;
        long endTime = System.currentTimeMillis();
        return Long.toString(endTime - startTime);
    }

    private void processFinishedSuites(@NotNull List<ServiceMessageBuilder> messages) {
        Iterator<String> iterator = pendingFinishedSuites.iterator();
        while (iterator.hasNext()) {
            String suite = iterator.next();
            Set<String> notFinished = suitesToNotFinishedChildren.get(suite);
            if (notFinished == null || notFinished.isEmpty()) {
                messages.add(createTestSuiteFinishedMessage(suite));
                iterator.remove();
            }
        }
    }

    private void recursivelyInitContainingSuite(@NotNull String node, @NotNull List<ServiceMessageBuilder> messages) {
        String suite = getParent(node);
        if (suite.equals(getTarget())) return;

        while (!suite.equals(getCurrentSuite()) && !suite.startsWith(getCurrentSuite() + NAME_SEPARATOR)) {
            String lastSuite = suitesStack.remove(suitesStack.size() - 1);
            pendingFinishedSuites.add(lastSuite);
        }
        processFinishedSuites(messages);

        if (suite.equals(getCurrentSuite())) return;

        recursivelyInitContainingSuite(suite, messages);

        messages.add(createTestSuiteStartedMessage(suite));
        recordSuiteChildStarted(suite);
        suitesStack.add(suite);
    }

    @NotNull
    private static String getNodeName(@NotNull String nodeId) {
        if (nodeId.contains(NAME_SEPARATOR)) {
            return nodeId.substring(nodeId.lastIndexOf(NAME_SEPARATOR) + NAME_SEPARATOR.length());
        } else {
            int lastDash = nodeId.lastIndexOf('-');
            String targetName = lastDash >= 0 ? nodeId.substring(0, lastDash) : nodeId;
            if (nodeId.endsWith(DOCTESTS_SUFFIX)) {
                return targetName + " (doc-tests)";
            }
            return targetName;
        }
    }

    @NotNull
    private static String getParent(@NotNull String nodeId) {
        int idx = nodeId.lastIndexOf(NAME_SEPARATOR);
        if (idx < 0) return ROOT_SUITE;
        String parent = nodeId.substring(0, idx);
        return parent.equals(nodeId) ? ROOT_SUITE : parent;
    }

    @NotNull
    private static ServiceMessageBuilder createTestSuiteStartedMessage(@NotNull String suite) {
        return ServiceMessageBuilder.testSuiteStarted(getNodeName(suite))
            .addAttribute("nodeId", suite)
            .addAttribute("parentNodeId", getParent(suite))
            .addAttribute("locationHint", CargoTestLocator.getTestUrl(suite));
    }

    @NotNull
    private static ServiceMessageBuilder createTestSuiteFinishedMessage(@NotNull String suite) {
        return ServiceMessageBuilder.testSuiteFinished(getNodeName(suite))
            .addAttribute("nodeId", suite);
    }

    @NotNull
    private static ServiceMessageBuilder createTestStartedMessage(@NotNull String test) {
        Matcher matcher = LINE_NUMBER_RE.matcher(test);
        StringBuffer sb2 = new StringBuffer();
        while (matcher.find()) {
            String line = matcher.group("line");
            if (line == null) throw new IllegalStateException("Failed to find `line` capturing group");
            matcher.appendReplacement(sb2, "#" + Integer.parseInt(line));
        }
        matcher.appendTail(sb2);
        String name = sb2.toString();

        return ServiceMessageBuilder.testStarted(getNodeName(test))
            .addAttribute("nodeId", test)
            .addAttribute("parentNodeId", getParent(test))
            .addAttribute("locationHint", CargoTestLocator.getTestUrl(name));
    }

    @NotNull
    private static ServiceMessageBuilder createTestFailedMessage(@NotNull String test, @NotNull String failedMessage, @Nullable SemVer rustcVersion) {
        ServiceMessageBuilder builder = ServiceMessageBuilder.testFailed(getNodeName(test))
            .addAttribute("nodeId", test)
            .addAttribute("details", failedMessage);
        ErrorMessage parseResult = parseErrorMessage(failedMessage, rustcVersion);
        if (parseResult == null) {
            builder.addAttribute("message", "");
        } else {
            builder.addAttribute("message", parseResult.message);
            if (parseResult.diff != null) {
                builder
                    .addAttribute("actual", parseResult.diff.left)
                    .addAttribute("expected", parseResult.diff.right);
            }
        }
        return builder;
    }

    @NotNull
    private static ServiceMessageBuilder createTestFinishedMessage(@NotNull String test, @NotNull String duration) {
        return ServiceMessageBuilder.testFinished(getNodeName(test))
            .addAttribute("nodeId", test)
            .addAttribute("duration", duration);
    }

    @NotNull
    private static ServiceMessageBuilder createTestIgnoredMessage(@NotNull String test) {
        return ServiceMessageBuilder.testIgnored(getNodeName(test))
            .addAttribute("nodeId", test);
    }

    @NotNull
    private static ServiceMessageBuilder createTestStdOutMessage(@NotNull String test, @NotNull String stdout) {
        return ServiceMessageBuilder.testStdOut(getNodeName(test))
            .addAttribute("nodeId", test)
            .addAttribute("out", stdout);
    }

    @NotNull
    private static ServiceMessageBuilder createTestCountMessage(@NotNull String testCount) {
        return new ServiceMessageBuilder("testCount")
            .addAttribute("count", testCount);
    }

    @NotNull
    private static FailedTestOutput parseFailedTestOutput(@NotNull String output) {
        String[] lines = output.split("\n", -1);
        StringBuilder stdout = new StringBuilder();
        StringBuilder failedMessage = new StringBuilder();
        boolean inFailed = false;
        for (String line : lines) {
            if (!inFailed) {
                String trimmedLine = line.stripLeading();
                if (trimmedLine.toLowerCase().startsWith("thread")) {
                    inFailed = true;
                }
            }
            if (inFailed) {
                if (failedMessage.length() > 0) failedMessage.append("\n");
                failedMessage.append(line);
            } else {
                if (stdout.length() > 0) stdout.append("\n");
                stdout.append(line);
            }
        }
        return new FailedTestOutput(stdout.toString(), failedMessage.toString());
    }

    @Nullable
    private static ErrorMessage parseErrorMessage(@NotNull String failedMessage, @Nullable SemVer rustcVersion) {
        Pattern regex = (rustcVersion == null || rustcVersion.compareTo(RUSTC_1_73_BETA) < 0)
            ? ERROR_MESSAGE_RE_OLD : ERROR_MESSAGE_RE;
        Matcher matcher = regex.matcher(failedMessage);
        if (!matcher.find()) return null;

        String message;
        try {
            message = matcher.group("message");
        } catch (IllegalArgumentException e) {
            message = null;
        }
        if (message == null || message.isEmpty()) {
            try {
                message = matcher.group("fullMessage");
            } catch (IllegalArgumentException e) {
                message = null;
            }
        }
        if (message == null) {
            throw new IllegalStateException("Failed to find `message` or `fullMessage` capturing group");
        }

        DiffResult diff = null;
        try {
            String sign = matcher.group("sign");
            if ("==".equals(sign)) {
                String left = matcher.group("left");
                String right = matcher.group("right");
                if (left != null) left = unescape(left);
                if (right != null) right = unescape(right);
                if (left != null && right != null) {
                    diff = new DiffResult(left, right);
                }
            }
        } catch (IllegalArgumentException ignored) {
        }

        return new ErrorMessage(message, diff, null);
    }

    @Nullable
    private static CargoWorkspace.Target findLibTargetForPackageName(@NotNull Project project, @NotNull String packageName) {
        for (CargoWorkspace.Package pkg : CargoProjectsServiceImplUtil.getAllPackages(CargoProjectServiceUtil.getCargoProjects(project))) {
            if (pkg.getOrigin() != PackageOrigin.WORKSPACE) continue;
            CargoWorkspace.Target libTarget = pkg.getLibTarget();
            if (libTarget == null) continue;
            if (packageName.equals(libTarget.getPkg().getName())) {
                return libTarget;
            }
        }
        return null;
    }

    @NotNull
    private static String unescape(@NotNull String s) {
        return StringUtil.unquoteString(StringUtil.unescapeStringCharacters(s));
    }

    private enum State {
        START_MESSAGE,
        EXECUTABLE_NAME,
        DOCTESTS_PACKAGE_NAME
    }

    private static final class FailedTestOutput {
        @NotNull final String stdout;
        @NotNull final String failedMessage;

        FailedTestOutput(@NotNull String stdout, @NotNull String failedMessage) {
            this.stdout = stdout;
            this.failedMessage = failedMessage;
        }
    }

    private static final class ErrorMessage {
        @NotNull final String message;
        @Nullable final DiffResult diff;
        @Nullable final String backtrace;

        ErrorMessage(@NotNull String message, @Nullable DiffResult diff, @Nullable String backtrace) {
            this.message = message;
            this.diff = diff;
            this.backtrace = backtrace;
        }
    }

    private static final class DiffResult {
        @NotNull final String left;
        @NotNull final String right;

        DiffResult(@NotNull String left, @NotNull String right) {
            this.left = left;
            this.right = right;
        }
    }

    static final class LibtestSuiteMessage {
        @NotNull final String type;
        @NotNull final String event;
        @NotNull final String testCount;

        LibtestSuiteMessage(@NotNull String type, @NotNull String event, @NotNull String testCount) {
            this.type = type;
            this.event = event;
            this.testCount = testCount;
        }

        @Nullable
        static LibtestSuiteMessage fromJson(@NotNull JsonObject json) {
            var typePrim = json.getAsJsonPrimitive("type");
            if (typePrim == null || !"suite".equals(typePrim.getAsString())) {
                return null;
            }
            LibtestSuiteMessageDto dto = new Gson().fromJson(json, LibtestSuiteMessageDto.class);
            return new LibtestSuiteMessage(dto.type, dto.event, dto.test_count);
        }
    }

    private static final class LibtestSuiteMessageDto {
        String type;
        String event;
        String test_count;
    }

    static final class LibtestTestMessage {
        @NotNull final String type;
        @NotNull final String event;
        @NotNull final String name;
        @Nullable final String stdout;

        LibtestTestMessage(@NotNull String type, @NotNull String event, @NotNull String name, @Nullable String stdout) {
            this.type = type;
            this.event = event;
            this.name = name;
            this.stdout = stdout;
        }

        @Nullable
        static LibtestTestMessage fromJson(@NotNull JsonObject json) {
            var typePrim = json.getAsJsonPrimitive("type");
            if (typePrim == null || !"test".equals(typePrim.getAsString())) {
                return null;
            }
            LibtestTestMessageDto dto = new Gson().fromJson(json, LibtestTestMessageDto.class);
            return new LibtestTestMessage(dto.type, dto.event, dto.name, dto.stdout);
        }
    }

    private static final class LibtestTestMessageDto {
        String type;
        String event;
        String name;
        String stdout;
    }

    static final class LibtestBenchMessage {
        @NotNull final String type;
        @NotNull final String name;
        @NotNull final String median;
        @NotNull final String deviation;

        LibtestBenchMessage(@NotNull String type, @NotNull String name, @NotNull String median, @NotNull String deviation) {
            this.type = type;
            this.name = name;
            this.median = median;
            this.deviation = deviation;
        }

        @Nullable
        static LibtestBenchMessage fromJson(@NotNull JsonObject json) {
            var typePrim = json.getAsJsonPrimitive("type");
            if (typePrim == null || !"bench".equals(typePrim.getAsString())) {
                return null;
            }
            LibtestBenchMessageDto dto = new Gson().fromJson(json, LibtestBenchMessageDto.class);
            return new LibtestBenchMessage(dto.type, dto.name, dto.median, dto.deviation);
        }
    }

    private static final class LibtestBenchMessageDto {
        String type;
        String name;
        String median;
        String deviation;
    }
}
