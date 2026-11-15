/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.language.codeStyle.FormatterUtil;
import consulo.language.codeStyle.FormattingContext;
import consulo.language.codeStyle.service.AsyncDocumentFormattingService;
import consulo.language.codeStyle.service.AsyncFormattingRequest;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.ProcessOutput;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationGroup;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.model.CargoProjectLocator;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RsToolchainLocator;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.ide.rustfmt.Rustfmt;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.notifications.RsNotifications;
import org.rust.openapiext.CommandLineExt;
import org.rust.openapiext.RsProcessExecutionException;
import org.rust.stdext.RsResult;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

/**
 * Reformat Code with rustfmt when the project asks for it. Only an explicit Reformat Code of a whole file: rustfmt
 * formats no fragments, and the platform formats on the fly and before commit with the built-in formatter.
 */
@ExtensionImpl
public class RustfmtFormattingService extends AsyncDocumentFormattingService {
    @Nonnull
    @Override
    public Set<Feature> getFeatures() {
        return Collections.emptySet();
    }

    @Override
    public boolean canFormat(@Nonnull PsiFile file) {
        return file instanceof RsFile
            && RsProjectSettingsServiceUtil.getRustfmtSettings(file.getProject()).getUseRustfmt()
            && isReformatCode();
    }

    private static boolean isReformatCode() {
        return FormatterUtil.REFORMAT_COMMAND_NAME.get().equals(CommandProcessor.getInstance().getCurrentCommandNameValue().get());
    }

    @Nullable
    @Override
    protected FormattingTask createFormattingTask(@Nonnull AsyncFormattingRequest request) {
        FormattingContext context = request.getContext();
        Project project = context.getProject();
        VirtualFile file = context.getVirtualFile();
        if (file == null) return null;
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) return null;
        CargoProject cargoProject = CargoProjectServiceUtil.getCargoProjects(project).findProjectForFile(file);
        if (cargoProject == null) return null;
        RsToolchainBase toolchain = RsToolchainLocator.getToolchain(project);
        if (toolchain == null) return null;
        Rustfmt rustfmt = Rustfmt.create(toolchain);

        return new FormattingTask() {
            // disposing it kills rustfmt
            private final Disposable myProcessOwner = Disposable.newDisposable("rustfmt");

            @Override
            public void run() {
                RustfmtTestmarks.RustfmtUsed.hit();
                try {
                    if (Rustup.checkNeedInstallRustfmt(project, CargoProjectLocator.getWorkingDirectory(cargoProject))) {
                        request.onTextReady(null);
                        return;
                    }
                    GeneralCommandLine commandLine = rustfmt.createCommandLine(cargoProject, document);
                    if (commandLine == null) {
                        request.onTextReady(null);
                        return;
                    }
                    // rustfmt reads the text to format from stdin and writes the result to stdout: no file is read
                    RsResult<ProcessOutput, RsProcessExecutionException> result = CommandLineExt.execute(
                        commandLine, myProcessOwner, request.getDocumentText().getBytes(StandardCharsets.UTF_8), null);
                    if (result instanceof RsResult.Ok<ProcessOutput, RsProcessExecutionException> ok) {
                        request.onTextReady(ok.getOk().getStdout());
                    }
                    else if (result instanceof RsResult.Err<ProcessOutput, RsProcessExecutionException> err
                        && !(err.getErr() instanceof RsProcessExecutionException.Canceled)) {
                        String message = err.getErr().getMessage();
                        request.onError(LocalizeValue.of(RsBundle.message("notification.title.rustfmt")),
                            LocalizeValue.of(message != null ? message.trim() : ""));
                    }
                }
                finally {
                    Disposer.dispose(myProcessOwner);
                }
            }

            @Override
            public boolean cancel() {
                Disposer.dispose(myProcessOwner);
                return true;
            }

            @Override
            public boolean isRunUnderProgress() {
                return true;
            }
        };
    }

    @Nonnull
    @Override
    protected NotificationGroup getNotificationGroup() {
        return RsNotifications.PLUGIN;
    }

    @Nonnull
    @Override
    protected LocalizeValue getName() {
        return LocalizeValue.of("rustfmt");
    }
}
