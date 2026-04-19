/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.google.gson.JsonObject;
import com.intellij.build.FilePosition;
import com.intellij.build.events.BuildEvent;
import com.intellij.build.events.MessageEvent;
import com.intellij.build.events.StartEvent;
import com.intellij.build.events.impl.*;
import com.intellij.build.output.BuildOutputInstantReader;
import com.intellij.build.output.BuildOutputParser;
import com.intellij.execution.process.AnsiEscapeDecoder;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.RsAnsiEscapeDecoder;
import org.rust.cargo.toolchain.impl.CargoMetadata;
import org.rust.cargo.toolchain.impl.RustcMessage;
import org.rust.openapiext.JsonUtils;
import org.rust.stdext.Utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("UnstableApiUsage")
public class RsBuildEventsConverter implements BuildOutputParser {
    private final CargoBuildContextBase context;
    private final AnsiEscapeDecoder decoder = new AnsiEscapeDecoder();
    private final List<StartEvent> startEvents = new ArrayList<>();
    private final Set<MessageEvent> messageEvents = new HashSet<>();
    private final StringBuilder jsonBuffer = new StringBuilder();

    private static final Pattern PROGRESS_TOTAL_RE = Pattern.compile("(\\d+)/(\\d+)");
    private static final List<String> MESSAGES_TO_IGNORE = List.of("Build failed, waiting for other jobs to finish", "Build failed");
    private static final List<MessageEvent.Kind> ERROR_OR_WARNING = List.of(MessageEvent.Kind.ERROR, MessageEvent.Kind.WARNING);

    public RsBuildEventsConverter(CargoBuildContextBase context) {
        this.context = context;
    }

    @Override
    public boolean parse(@NotNull String line, @NotNull BuildOutputInstantReader reader, @NotNull Consumer<? super BuildEvent> messageConsumer) {
        if (jsonBuffer.isEmpty() && !line.contains("{\"reason\"")) {
            return tryHandleCargoMessage(RsAnsiEscapeDecoder.quantizeAnsiColors(line), messageConsumer);
        }

        jsonBuffer.append(withNewLine(line));
        String message = jsonBuffer.toString();
        int braceIdx = message.indexOf('{');
        if (braceIdx > 0) {
            message = message.substring(braceIdx);
        }
        JsonObject jsonObject = JsonUtils.INSTANCE.tryParseJsonObject(message, false);
        if (jsonObject == null) return false;
        jsonBuffer.setLength(0);

        return tryHandleRustcMessage(jsonObject, messageConsumer) || tryHandleRustcArtifact(jsonObject);
    }

    private boolean tryHandleRustcMessage(JsonObject jsonObject, Consumer<? super BuildEvent> messageConsumer) {
        RustcMessage.CargoTopMessage topMessage = RustcMessage.CargoTopMessage.fromJson(jsonObject);
        if (topMessage == null) return false;
        RustcMessage.RustcDiagnostic rustcMessage = topMessage.getMessage();

        String detailedMessage = rustcMessage.getRendered() != null
            ? RsAnsiEscapeDecoder.quantizeAnsiColors(rustcMessage.getRendered())
            : null;
        if (detailedMessage != null) {
            acceptText(messageConsumer, context.getParentId(), withNewLine(detailedMessage));
        }

        String msg = Utils.capitalized(rustcMessage.getMessage().trim());
        if (msg.endsWith(".")) msg = msg.substring(0, msg.length() - 1);
        if (msg.startsWith("Aborting due") || msg.endsWith("emitted")) return true;

        String parentEventId = topMessage.getPackageId();
        int parenIdx = parentEventId.indexOf('(');
        if (parenIdx > 0) parentEventId = parentEventId.substring(0, parenIdx).trim();
        final String finalParentEventId = parentEventId;

        MessageEvent.Kind kind = getMessageKind(rustcMessage.getLevel());
        if (kind == MessageEvent.Kind.SIMPLE) return true;

        FilePosition filePosition = getFilePosition(rustcMessage);
        MessageEvent messageEvent = createMessageEvent(context.getWorkingDirectory(), finalParentEventId, kind, msg, detailedMessage, filePosition);
        if (messageEvents.add(messageEvent)) {
            boolean hasParent = startEvents.stream().anyMatch(e -> finalParentEventId.equals(e.getId()));
            if (!hasParent) {
                handleCompilingMessage(RsBundle.message("build.event.message.compiling.0", parentEventId), false, messageConsumer);
            }
            messageConsumer.accept(messageEvent);
            if (kind == MessageEvent.Kind.ERROR) {
                context.getErrors().incrementAndGet();
                if (rustcMessage.getCode() != null && rustcMessage.getCode().getCode() != null) {
                    context.getErrorCodes().add(rustcMessage.getCode().getCode());
                }
            } else {
                context.getWarnings().incrementAndGet();
            }
        }
        return true;
    }

    private boolean tryHandleRustcArtifact(JsonObject jsonObject) {
        RustcMessage.CompilerArtifactMessage rustcArtifact = RustcMessage.CompilerArtifactMessage.fromJson(jsonObject);
        if (rustcArtifact == null) return false;

        boolean isSuitableTarget;
        switch (rustcArtifact.getTarget().getCleanKind()) {
            case BIN:
                isSuitableTarget = true;
                break;
            case EXAMPLE:
                isSuitableTarget = rustcArtifact.getTarget().getCleanCrateTypes().size() == 1 &&
                    rustcArtifact.getTarget().getCleanCrateTypes().get(0) == CargoMetadata.CrateType.BIN;
                break;
            case TEST:
            case BENCH:
                isSuitableTarget = true;
                break;
            case LIB:
                isSuitableTarget = rustcArtifact.getProfile().isTest();
                break;
            default:
                isSuitableTarget = false;
        }
        if (!isSuitableTarget || (context.isTestBuild() && !rustcArtifact.getProfile().isTest())) return true;

        List<RustcMessage.CompilerArtifactMessage> newArtifacts = new ArrayList<>(context.getArtifacts());
        newArtifacts.add(rustcArtifact);
        context.setArtifacts(newArtifacts);

        return true;
    }

    private boolean tryHandleCargoMessage(String line, Consumer<? super BuildEvent> messageConsumer) {
        String cleanLine = RsAnsiEscapeDecoder.removeEscapeSequences(decoder, line);
        if (cleanLine.isEmpty()) return true;

        String prefix = cleanLine.contains(":") ? cleanLine.substring(0, cleanLine.indexOf(':')) : cleanLine;
        MessageEvent.Kind kind = getMessageKind(prefix);
        String message = cleanLine;
        if (ERROR_OR_WARNING.contains(kind)) {
            message = cleanLine.substring(cleanLine.indexOf(':') + 1);
        }
        String icePrefix = RsBundle.message("build.event.message.internal.compiler.error");
        if (message.startsWith(icePrefix)) {
            message = message.substring(icePrefix.length());
        }
        message = Utils.capitalized(message.trim());
        if (message.endsWith(".")) message = message.substring(0, message.length() - 1);

        if (message.startsWith("Compiling") || message.startsWith("Checking")) {
            handleCompilingMessage(message, false, messageConsumer);
        } else if (message.startsWith("Fresh")) {
            handleCompilingMessage(message, true, messageConsumer);
        } else if (message.startsWith("Building")) {
            handleProgressMessage(cleanLine, messageConsumer);
            return true;
        } else if (message.startsWith("Downloading") || message.startsWith("Checkout") || message.startsWith("Fetch")) {
            return true;
        } else if (message.startsWith("Finished")) {
            handleFinishedMessage(null, messageConsumer);
        } else if (message.startsWith("Could not compile")) {
            String taskName = message.substring(message.indexOf('`') + 1);
            taskName = taskName.substring(0, taskName.indexOf('`'));
            handleFinishedMessage(taskName, messageConsumer);
        } else if (ERROR_OR_WARNING.contains(kind)) {
            handleProblemMessage(kind, message, line, messageConsumer);
        }

        acceptText(messageConsumer, context.getParentId(), withNewLine(line));
        return true;
    }

    private void handleCompilingMessage(String originalMessage, boolean isUpToDate, Consumer<? super BuildEvent> messageConsumer) {
        String message = originalMessage
            .replace(RsBundle.message("build.event.message.fresh"), RsBundle.message("build.event.message.compiling"));
        int parenIdx = message.indexOf('(');
        if (parenIdx > 0) message = message.substring(0, parenIdx).trim();

        String eventId = message.contains(" ") ? message.substring(message.indexOf(' ') + 1).replace(" v", " ") : message;
        StartEventImpl startEvent = new StartEventImpl(eventId, context.getParentId(), System.currentTimeMillis(), message);
        messageConsumer.accept(startEvent);
        if (isUpToDate) {
            FinishEventImpl finishEvent = new FinishEventImpl(
                eventId, context.getParentId(), System.currentTimeMillis(), message, new SuccessResultImpl(true)
            );
            messageConsumer.accept(finishEvent);
        } else {
            startEvents.add(startEvent);
        }
    }

    private void handleProgressMessage(String message, Consumer<? super BuildEvent> messageConsumer) {
        String afterColon = message.contains(":") ? message.substring(message.indexOf(':') + 1) : message;
        String[] parts = afterColon.split(",");
        List<String> activeTaskNames = new ArrayList<>();
        for (String part : parts) {
            String name = part.contains("(") ? part.substring(0, part.indexOf('(')).trim() : part.trim();
            activeTaskNames.add(name);
        }

        // Finish non-active tasks
        Iterator<StartEvent> iterator = startEvents.iterator();
        while (iterator.hasNext()) {
            StartEvent startEvent = iterator.next();
            String taskName = getTaskName(startEvent);
            if (taskName != null && !activeTaskNames.contains(taskName)) {
                iterator.remove();
                FinishEventImpl finishEvent = new FinishEventImpl(
                    startEvent.getId(), context.getParentId(), System.currentTimeMillis(),
                    startEvent.getMessage(), new SuccessResultImpl()
                );
                messageConsumer.accept(finishEvent);
            }
        }

        // Parse progress
        Matcher progressMatcher = PROGRESS_TOTAL_RE.matcher(message);
        long current = -1, total = -1;
        if (progressMatcher.find()) {
            try {
                current = Long.parseLong(progressMatcher.group(1));
                total = Long.parseLong(progressMatcher.group(2));
            } catch (NumberFormatException ignore) {
            }
        }

        ProgressIndicator indicator = context.getIndicator();
        if (indicator != null) {
            indicator.setIndeterminate(total < 0);
            indicator.setText(context.getProgressTitle());
            indicator.setText2(afterColon.trim());
            if (total > 0) {
                indicator.setFraction((double) current / total);
            }
        }
    }

    private void handleFinishedMessage(String failedTaskName, Consumer<? super BuildEvent> messageConsumer) {
        for (StartEvent startEvent : startEvents) {
            String taskName = getTaskName(startEvent);
            Object result;
            if (failedTaskName != null && failedTaskName.equals(taskName)) {
                result = new FailureResultImpl((Throwable) null);
            } else if (failedTaskName == null) {
                result = new SuccessResultImpl();
            } else {
                result = new SkippedResultImpl();
            }
            FinishEventImpl finishEvent = new FinishEventImpl(
                startEvent.getId(), context.getParentId(), System.currentTimeMillis(),
                startEvent.getMessage(), (com.intellij.build.events.EventResult) result
            );
            messageConsumer.accept(finishEvent);
        }
    }

    private void handleProblemMessage(MessageEvent.Kind kind, String message, String detailedMessage, Consumer<? super BuildEvent> messageConsumer) {
        if (MESSAGES_TO_IGNORE.contains(message)) return;
        MessageEvent messageEvent = createMessageEvent(context.getWorkingDirectory(), context.getParentId(), kind, message, detailedMessage, null);
        if (messageEvents.add(messageEvent)) {
            messageConsumer.accept(messageEvent);
            if (kind == MessageEvent.Kind.ERROR) {
                context.getErrors().incrementAndGet();
            } else {
                context.getWarnings().incrementAndGet();
            }
        }
    }

    private FilePosition getFilePosition(RustcMessage.RustcDiagnostic message) {
        RustcMessage.RustcSpan span = message.getMainSpan();
        if (span == null) return null;
        Path filePath = Paths.get(span.getFile_name());
        if (!filePath.isAbsolute()) {
            filePath = context.getWorkingDirectory().resolve(filePath);
        }
        return new FilePosition(
            filePath.toFile(),
            span.getLine_start() - 1,
            span.getColumn_start() - 1,
            span.getLine_end() - 1,
            span.getColumn_end() - 1
        );
    }

    private static String getTaskName(StartEvent event) {
        Object id = event.getId();
        if (id instanceof String str) {
            String name = str.contains(" ") ? str.substring(0, str.indexOf(' ')) : str;
            return name.contains("(") ? name.substring(0, name.indexOf('(')).trim() : name;
        }
        return null;
    }

    private static String withNewLine(String s) {
        return StringUtil.endsWithLineBreak(s) ? s : s + '\n';
    }

    private static void acceptText(Consumer<? super BuildEvent> messageConsumer, Object parentId, String text) {
        messageConsumer.accept(new OutputBuildEventImpl(parentId, text, true));
    }

    private static MessageEvent.Kind getMessageKind(String kind) {
        return switch (kind) {
            case "error", "error: internal compiler error" -> MessageEvent.Kind.ERROR;
            case "warning" -> MessageEvent.Kind.WARNING;
            default -> MessageEvent.Kind.SIMPLE;
        };
    }

    private static MessageEvent createMessageEvent(
        Path workingDirectory,
        Object parentEventId,
        MessageEvent.Kind kind,
        String message,
        String detailedMessage,
        FilePosition filePosition
    ) {
        return new FileMessageEventImpl(
            parentEventId,
            kind,
            RsBundle.message("rust.compiler"),
            message,
            detailedMessage,
            filePosition != null ? filePosition : new FilePosition(workingDirectory.toFile(), 0, 0)
        );
    }
}
