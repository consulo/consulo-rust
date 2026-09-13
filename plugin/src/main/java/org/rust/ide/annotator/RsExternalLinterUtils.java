/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import org.rust.stdext.Lazy;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.inspection.SuppressIntentionActionFromFix;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.disposer.Disposable;
import consulo.application.WriteAction;
import consulo.logging.Logger;
import consulo.document.Document;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.document.util.TextRange;
import consulo.project.ui.wm.WindowManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.AnyPsiChangeListener;
import consulo.language.psi.PsiModificationTracker;
import consulo.util.io.PathUtil;
import consulo.util.io.URLUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.CargoCheckArgs;
import org.rust.cargo.toolchain.tools.CargoExtUtil;
import org.rust.ide.fixes.ApplySuggestionFix;
import org.rust.ide.inspections.lints.RsLint;
import org.rust.ide.inspections.lints.RsSuppressQuickFix;
import org.rust.ide.status.RsExternalLinterWidget;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.openapiext.*;
import org.rust.openapiext.JsonUtils;
import org.rust.stdext.StdextUtil;
import org.rust.stdext.RsResult;
import consulo.process.ExecutionException;
import consulo.process.util.ProcessOutput;
import org.rust.cargo.toolchain.tools.Cargo;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.rust.cargo.api.toolchain.RustcMessage;
import consulo.component.messagebus.MessageBus;
import consulo.language.editor.intention.IntentionAction;
import consulo.localize.LocalizeValue;
import consulo.project.ui.wm.StatusBar;

public final class RsExternalLinterUtils {
    public static final RsExternalLinterUtils INSTANCE = new RsExternalLinterUtils();

    private static final Logger LOG = Logger.getInstance(RsExternalLinterUtils.class);
    public static final String TEST_MESSAGE = "RsExternalLint";
    private static final String RUST_EXTERNAL_LINTER_ID = "RsExternalLinterOptions";
    private static final Pattern ERROR_REGEX = Pattern.compile("E\\d{4}");
    private static final Pattern URL_REGEX = URLUtil.URL_PATTERN;
    private static final Pattern MESSAGE_REGEX = Pattern.compile("\\s*\\{.*\"message\".*");

    private RsExternalLinterUtils() {
    }

    /**
     * Returns (and caches if absent) lazily computed messages from the external linter.
     *
     * Note: before applying the result, check that the PSI modification stamp of the project has not
     * changed since this method was called.
     *
     * @see PsiModificationTracker#MODIFICATION_COUNT
     */
    @Nonnull
    public static Lazy<RsExternalLinterResult> checkLazily(
        @Nonnull RsToolchainBase toolchain,
        @Nonnull Project project,
        @Nonnull Disposable owner,
        @Nonnull Path workingDirectory,
        @Nonnull CargoCheckArgs args
    ) {
        OpenApiUtil.checkReadAccessAllowed();
        return externalLinterLazyResultCache.getOrPut(project, new Key(toolchain, workingDirectory, args), () ->
            // The linter runs in a background thread *without* a read action. Its result is cached because
            // it is cargo-package-global while the annotator runs per file. The cached value must be
            // invalidated on any PSI change, and the modification count is recorded AFTER the value is
            // computed - so computing outside a read action would cache a value that is already outdated.
            // Storing a `Lazy` instead sidesteps that: the `Lazy` itself is created inside the read action
            // (and so recorded against the right modification count), while its value is computed later,
            // in a background thread.
            new Lazy<>(() -> {
                if (!OpenApiUtil.isUnitTestMode()) {
                    OpenApiUtil.checkReadAccessNotAllowed();
                }
                return checkWrapped(toolchain, project, owner, workingDirectory, args);
            })
        );
    }

    @Nullable
    private static RsExternalLinterResult checkWrapped(
        @Nonnull RsToolchainBase toolchain,
        @Nonnull Project project,
        @Nonnull Disposable owner,
        @Nonnull Path workingDirectory,
        @Nonnull CargoCheckArgs args
    ) {
        RsExternalLinterWidget widget = WriteAction.computeAndWait(() -> {
            org.rust.ide.rustfmt.RustfmtWatcher.saveAllDocumentsAsTheyAre();
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            if (statusBar == null) return null;
            return statusBar.<RsExternalLinterWidget>findWidget(w -> w instanceof RsExternalLinterWidget).orElse(null);
        });

        CompletableFuture<RsExternalLinterResult> future = new CompletableFuture<>();
        Task.Backgroundable task = new Task.Backgroundable(
            project,
            RsBundle.message("progress.title.analyzing.project.with", args.getLinter().getTitle()),
            true
        ) {
            @Override
            public void run(@Nonnull ProgressIndicator indicator) {
                if (widget != null) widget.setInProgress(true);
                try {
                    future.complete(check(toolchain, project, owner, workingDirectory, args));
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                    throw e;
                }
            }

            @Override
            public void onFinished() {
                if (widget != null) widget.setInProgress(false);
            }
        };
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, new EmptyProgressIndicator());
        try {
            return future.get();
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            if (cause instanceof Error) throw (Error) cause;
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Nullable
    private static RsExternalLinterResult check(
        @Nonnull RsToolchainBase toolchain,
        @Nonnull Project project,
        @Nonnull Disposable owner,
        @Nonnull Path workingDirectory,
        @Nonnull CargoCheckArgs args
    ) {
        ProgressManager.checkCanceled();
        Instant started = Instant.now();
        RsResult<ProcessOutput, RsProcessExecutionException.Start> result =
            Cargo.cargoOrWrapper(toolchain, workingDirectory).checkProject(project, owner, args);
        if (result instanceof RsResult.Err) {
            LOG.error(String.valueOf(((RsResult.Err<?, ?>) result).getErr()));
            return null;
        }
        ProcessOutput output = ((RsResult.Ok<ProcessOutput, ?>) result).getOk();
        Instant finish = Instant.now();
        ProgressManager.checkCanceled();
        if (output.isCancelled()) return null;
        return new RsExternalLinterResult(output.getStdoutLines(), Duration.between(started, finish).toMillis());
    }

    private static final class Key {
        private final RsToolchainBase toolchain;
        private final Path workingDirectory;
        private final CargoCheckArgs args;

        Key(RsToolchainBase toolchain, Path workingDirectory, CargoCheckArgs args) {
            this.toolchain = toolchain;
            this.workingDirectory = workingDirectory;
            this.args = args;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key)) return false;
            Key key = (Key) o;
            return Objects.equals(toolchain, key.toolchain)
                && Objects.equals(workingDirectory, key.workingDirectory)
                && Objects.equals(args, key.args);
        }

        @Override
        public int hashCode() {
            return Objects.hash(toolchain, workingDirectory, args);
        }
    }

    private static final ProjectCache<Key, Lazy<RsExternalLinterResult>> externalLinterLazyResultCache =
        new ProjectCache<>("externalLinterLazyResultCache", project -> PsiModificationTracker.MODIFICATION_COUNT);

    @Nonnull
    public static Disposable createDisposableOnAnyPsiChange(@Nonnull consulo.component.messagebus.MessageBus messageBus) {
        Disposable disposable = Disposable.newDisposable("Dispose on PSI change");
        messageBus.connect(disposable).subscribe(
            AnyPsiChangeListener.class,
            new AnyPsiChangeListener() {
                @Override
                public void beforePsiChanged(boolean isPhysical) {
                    if (isPhysical) {
                        Disposer.dispose(disposable);
                    }
                }
            }
        );
        return disposable;
    }

    public static void addHighlightsForFile(
        @Nonnull List<HighlightInfo> highlights,
        @Nonnull RsFile file,
        @Nonnull RsExternalLinterResult annotationResult,
        @Nonnull RustcMessage.Applicability minApplicability
    ) {
        PackageOrigin cargoPackageOrigin = RsElementUtil.getContainingCargoPackage(file) != null
            ? RsElementUtil.getContainingCargoPackage(file).getOrigin() : null;
        if (cargoPackageOrigin != PackageOrigin.WORKSPACE) return;

        Document doc = file.getViewProvider().getDocument();
        if (doc == null) {
            throw new IllegalStateException("Can't find document for " + file + " in external linter");
        }

        List<FilteredMessage> filteredMessages = new ArrayList<>();
        for (RustcMessage.CargoTopMessage topMessage : annotationResult.getMessages()) {
            FilteredMessage message = filterMessage(file, doc, topMessage.getMessage());
            // Cargo can duplicate some error messages when `--all-targets` is used
            if (message != null && !filteredMessages.contains(message)) {
                filteredMessages.add(message);
            }
        }

        for (FilteredMessage message : filteredMessages) {
            // The messages cargo generates can't be controlled, so they can't be tested well.
            // A dedicated message is used in tests to tell external-linter annotations apart.
            HighlightInfo.Builder highlightBuilder = HighlightInfo.newHighlightInfo(convertSeverity(message.severity))
                .severity(message.severity)
                .description(OpenApiUtil.isUnitTestMode() ? TEST_MESSAGE : message.message)
                .escapedToolTip(LocalizeValue.of(message.htmlTooltip))
                .range(message.textRange)
                .needsUpdateOnTyping(true);

            ApplySuggestionFix singleFix = null;
            for (ApplySuggestionFix fix : message.quickFixes) {
                if (fix.getApplicability().compareTo(minApplicability) <= 0) {
                    if (singleFix != null) {
                        singleFix = null;
                        break;
                    }
                    singleFix = fix;
                }
            }
            if (singleFix != null) {
                PsiElement element = singleFix.getStartElement() != null ? singleFix.getStartElement() : singleFix.getEndElement();
                List<IntentionAction> options = new ArrayList<>();
                if (element != null && message.lint != null) {
                    RsSuppressQuickFix[] actions = RsSuppressQuickFix.createSuppressFixes(element, message.lint);
                    Collections.addAll(options, SuppressIntentionActionFromFix.convertBatchToSuppressIntentionActions(actions));
                }
                String displayName = RsBundle.message("rust.external.linter");
                HighlightDisplayKey key = HighlightDisplayKey.find(RUST_EXTERNAL_LINTER_ID);
                if (key == null) key = new HighlightDisplayKey(displayName, RUST_EXTERNAL_LINTER_ID);
                highlightBuilder = highlightBuilder.registerFix(
                    singleFix, options, LocalizeValue.of(displayName), singleFix.getTextRange(), key);
            }

            HighlightInfo info = highlightBuilder.create();
            if (info != null) highlights.add(info);
        }
    }

    /** A rustc message mapped onto a range of {@code file}, ready to become a highlight. */
    private static final class FilteredMessage {
        private final HighlightSeverity severity;
        private final TextRange textRange;
        private final String message;
        private final String htmlTooltip;
        @Nullable
        private final RsLint.ExternalLinterLint lint;
        private final List<ApplySuggestionFix> quickFixes;

        FilteredMessage(
            HighlightSeverity severity,
            TextRange textRange,
            String message,
            String htmlTooltip,
            @Nullable RsLint.ExternalLinterLint lint,
            List<ApplySuggestionFix> quickFixes
        ) {
            this.severity = severity;
            this.textRange = textRange;
            this.message = message;
            this.htmlTooltip = htmlTooltip;
            this.lint = lint;
            this.quickFixes = quickFixes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FilteredMessage)) return false;
            FilteredMessage that = (FilteredMessage) o;
            return Objects.equals(severity, that.severity)
                && Objects.equals(textRange, that.textRange)
                && Objects.equals(message, that.message)
                && Objects.equals(htmlTooltip, that.htmlTooltip)
                && Objects.equals(lint, that.lint);
        }

        @Override
        public int hashCode() {
            return Objects.hash(severity, textRange, message, htmlTooltip, lint);
        }
    }

    @Nullable
    private static FilteredMessage filterMessage(
        @Nonnull PsiFile file,
        @Nonnull Document document,
        @Nonnull RustcMessage.RustcDiagnostic message
    ) {
        String text = message.getMessage();
        if (text.startsWith("aborting due to") || text.startsWith("cannot continue")) return null;

        HighlightSeverity severity;
        if ("error".equals(message.getLevel())) {
            severity = HighlightSeverity.ERROR;
        } else if ("warning".equals(message.getLevel())) {
            severity = HighlightSeverity.WEAK_WARNING;
        } else {
            severity = HighlightSeverity.INFORMATION;
        }

        // Some messages are global; they would have to be shown atop the editor, which looks bad, so skip them.
        RustcMessage.RustcSpan span = message.getMainSpan();
        if (span == null) return null;

        String label = span.getLabel() != null ? span.getLabel() : "";
        for (String syntaxError : List.of("expected pattern", "unexpected token")) {
            if (label.contains(syntaxError) || text.contains(syntaxError)) return null;
        }

        String spanFilePath = PathUtil.toSystemIndependentName(span.getFile_name());
        if (file.getVirtualFile() == null || !file.getVirtualFile().getPath().endsWith(spanFilePath)) return null;

        TextRange textRange = span.toTextRange(document);
        if (textRange == null) return null;

        StringBuilder tooltip = new StringBuilder();
        tooltip.append(escapeUrls(formatMessage(StringEscapeUtils.escapeHtml4(text))));
        String code = formatAsLink(message.getCode());
        if (code != null) {
            tooltip.append(" [").append(code).append("]");
        }

        List<String> extraLines = new ArrayList<>();
        if (span.getLabel() != null && !text.startsWith(span.getLabel())) {
            extraLines.add(StringEscapeUtils.escapeHtml4(span.getLabel()));
        }
        for (RustcMessage.RustcDiagnostic child : message.getChildren()) {
            if (child.getMessage() != null && !child.getMessage().isBlank()) {
                extraLines.add(capitalized(child.getLevel()) + ": " + StringEscapeUtils.escapeHtml4(child.getMessage()));
            }
        }
        StringBuilder extra = new StringBuilder();
        for (String line : extraLines) {
            extra.append("<br>").append(formatMessage(line));
        }
        tooltip.append(escapeUrls(extra.toString()));

        RsLint.ExternalLinterLint lint = null;
        if (message.getCode() != null && message.getCode().getCode() != null) {
            lint = new RsLint.ExternalLinterLint(message.getCode().getCode());
        }

        return new FilteredMessage(
            severity,
            textRange,
            capitalized(text),
            tooltip.toString(),
            lint,
            collectQuickFixes(message, file, document)
        );
    }

    @Nonnull
    private static List<ApplySuggestionFix> collectQuickFixes(
        @Nonnull RustcMessage.RustcDiagnostic message,
        @Nonnull PsiFile file,
        @Nonnull Document document
    ) {
        List<ApplySuggestionFix> quickFixes = new ArrayList<>();
        collectQuickFixes(message, file, document, quickFixes);
        return quickFixes;
    }

    private static void collectQuickFixes(
        @Nonnull RustcMessage.RustcDiagnostic message,
        @Nonnull PsiFile file,
        @Nonnull Document document,
        @Nonnull List<ApplySuggestionFix> acc
    ) {
        RustcMessage.RustcSpan primary = null;
        for (RustcMessage.RustcSpan span : message.getSpans()) {
            if (span.isIs_primary() && isValid(span)) {
                if (primary != null) {
                    primary = null;
                    break;
                }
                primary = span;
            }
        }
        ApplySuggestionFix fix = createQuickFix(file, document, primary, message.getMessage());
        if (fix != null) acc.add(fix);
        for (RustcMessage.RustcDiagnostic child : message.getChildren()) {
            collectQuickFixes(child, file, document, acc);
        }
    }

    @Nullable
    private static ApplySuggestionFix createQuickFix(
        @Nonnull PsiFile file,
        @Nonnull Document document,
        @Nullable RustcMessage.RustcSpan span,
        @Nonnull String message
    ) {
        if (span == null || span.getSuggested_replacement() == null || span.getSuggestion_applicability() == null) return null;
        TextRange textRange = span.toTextRange(document);
        if (textRange == null) return null;
        PsiElement endElement = file.findElementAt(textRange.getEndOffset() - 1);
        if (endElement == null) return null;
        PsiElement startElement = file.findElementAt(textRange.getStartOffset());
        if (startElement == null) startElement = endElement;
        return new ApplySuggestionFix(
            message,
            span.getSuggested_replacement(),
            span.getSuggestion_applicability(),
            startElement,
            endElement,
            textRange
        );
    }

    @Nullable
    private static String formatAsLink(@Nullable RustcMessage.ErrorCode errorCode) {
        if (errorCode == null || errorCode.getCode() == null) return null;
        if (!ERROR_REGEX.matcher(errorCode.getCode()).matches()) return null;
        return "<a href=\"" + RsConstants.ERROR_INDEX_URL + "#" + errorCode.getCode() + "\">" + errorCode.getCode() + "</a>";
    }

    @Nonnull
    private static String escapeUrls(@Nonnull String text) {
        Matcher matcher = URL_REGEX.matcher(text);
        StringBuilder result = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            result.append(text, last, matcher.start());
            String url = matcher.group();
            result.append("<a href='").append(url).append("'>").append(url).append("</a>");
            last = matcher.end();
        }
        result.append(text.substring(last));
        return result.toString();
    }

    @Nonnull
    private static String capitalized(@Nonnull String text) {
        return text.isEmpty() ? text : Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /**
     * Renders the plain-text rustc message as HTML: consecutive {@code - } lines become one list,
     * other consecutive lines are joined with line breaks.
     */
    @Nonnull
    private static String formatMessage(@Nonnull String message) {
        List<Boolean> groupIsList = new ArrayList<>();
        List<List<String>> groupLines = new ArrayList<>();
        for (String lineWithPrefix : message.split("\n", -1)) {
            boolean isListItem = lineWithPrefix.startsWith("-");
            String line = isListItem ? lineWithPrefix.substring(Math.min(2, lineWithPrefix.length())) : lineWithPrefix;
            if (!groupIsList.isEmpty() && groupIsList.get(groupIsList.size() - 1) == isListItem) {
                groupLines.get(groupLines.size() - 1).add(line);
            } else {
                groupIsList.add(isListItem);
                groupLines.add(new ArrayList<>(List.of(line)));
            }
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < groupIsList.size(); i++) {
            if (i > 0) result.append(", ");
            List<String> lines = groupLines.get(i);
            if (groupIsList.get(i)) {
                result.append("<ul><li>").append(String.join("<li>", lines)).append("</ul>");
            } else {
                result.append(String.join("<br>", lines));
            }
        }
        return result.toString();
    }

    @Nonnull
    private static HighlightInfoType convertSeverity(@Nonnull HighlightSeverity severity) {
        if (severity == HighlightSeverity.ERROR) return HighlightInfoType.ERROR;
        if (severity == HighlightSeverity.WARNING) return HighlightInfoType.WARNING;
        if (severity == HighlightSeverity.WEAK_WARNING) return HighlightInfoType.WEAK_WARNING;
        if (severity == HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING) return HighlightInfoType.GENERIC_WARNINGS_OR_ERRORS_FROM_SERVER;
        return HighlightInfoType.INFORMATION;
    }

    public static boolean isValid(@Nonnull RustcMessage.RustcSpan span) {
        return span.getLine_end() > span.getLine_start()
            || (span.getLine_end() == span.getLine_start() && span.getColumn_end() >= span.getColumn_start());
    }
}
