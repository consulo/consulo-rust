/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import org.rust.stdext.Lazy;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInspection.SuppressIntentionActionFromFix;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.AnyPsiChangeListener;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.PathUtil;
import com.intellij.util.io.URLUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.impl.*;
import org.rust.cargo.toolchain.tools.CargoCheckArgs;
import org.rust.cargo.toolchain.tools.CargoExtUtil;
import org.rust.ide.fixes.ApplySuggestionFix;
import org.rust.ide.inspections.lints.RsLint;
import org.rust.ide.inspections.lints.RsSuppressQuickFix;
import org.rust.ide.status.RsExternalLinterWidget;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.openapiext.*;
import org.rust.openapiext.JsonUtils;
import org.rust.stdext.StdextUtil;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.rust.cargo.toolchain.impl.RustcMessage;

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

    @Nullable
    public static Lazy<RsExternalLinterResult> checkLazily(
        @NotNull Object toolchain,
        @NotNull Project project,
        @NotNull Disposable owner,
        @NotNull CargoCheckArgs args
    ) {
        return new Lazy<>(() -> {
            // Simplified - returns null for now
            return null;
        });
    }

    @NotNull
    public static Disposable createDisposableOnAnyPsiChange(@NotNull com.intellij.util.messages.MessageBus messageBus) {
        Disposable disposable = Disposer.newDisposable("Dispose on PSI change");
        messageBus.connect(disposable).subscribe(
            PsiManagerImpl.ANY_PSI_CHANGE_TOPIC,
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
        @NotNull List<HighlightInfo> highlights,
        @NotNull RsFile file,
        @NotNull RsExternalLinterResult annotationResult,
        @NotNull RustcMessage.Applicability minApplicability
    ) {
        PackageOrigin cargoPackageOrigin = RsElementUtil.getContainingCargoPackage(file) != null
            ? RsElementUtil.getContainingCargoPackage(file).getOrigin() : null;
        if (cargoPackageOrigin != PackageOrigin.WORKSPACE) return;

        Document doc = file.getViewProvider().getDocument();
        if (doc == null) {
            throw new IllegalStateException("Can't find document for " + file + " in external linter");
        }

        // Simplified: Process messages from annotationResult
        for (RustcMessage.CargoTopMessage topMessage : annotationResult.getMessages()) {
            // filtering and processing would go here
        }
    }

    @NotNull
    private static HighlightInfoType convertSeverity(@NotNull HighlightSeverity severity) {
        if (severity == HighlightSeverity.ERROR) return HighlightInfoType.ERROR;
        if (severity == HighlightSeverity.WARNING) return HighlightInfoType.WARNING;
        if (severity == HighlightSeverity.WEAK_WARNING) return HighlightInfoType.WEAK_WARNING;
        if (severity == HighlightSeverity.GENERIC_SERVER_ERROR_OR_WARNING) return HighlightInfoType.GENERIC_WARNINGS_OR_ERRORS_FROM_SERVER;
        return HighlightInfoType.INFORMATION;
    }

    public static boolean isValid(@NotNull RustcMessage.RustcSpan span) {
        return span.getLine_end() > span.getLine_start()
            || (span.getLine_end() == span.getLine_start() && span.getColumn_end() >= span.getColumn_start());
    }
}
