/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.formatting.FormattingContext;
import com.intellij.formatting.service.AsyncDocumentFormattingService;
import com.intellij.formatting.service.AsyncFormattingRequest;
import com.intellij.formatting.service.FormattingService;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.formatter.FormatterUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Rustfmt;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.CommandLineExt;
import org.rust.stdext.RsResult;

import java.util.Collections;
import java.util.Set;

public class RustfmtFormattingService extends AsyncDocumentFormattingService {

    private static final Set<FormattingService.Feature> FEATURES = Collections.emptySet();

    @NotNull
    @Override
    public Set<Feature> getFeatures() {
        return FEATURES;
    }

    @Override
    public boolean canFormat(@NotNull PsiFile file) {
        return file instanceof RsFile
            && RsProjectSettingsServiceUtil.getRustfmtSettings(file.getProject()).getUseRustfmt()
            && getFormattingReason() == FormattingReason.ReformatCode;
    }

    @Nullable
    @Override
    protected FormattingTask createFormattingTask(@NotNull AsyncFormattingRequest request) {
        FormattingContext context = request.getContext();
        Project project = context.getProject();
        VirtualFile file = context.getVirtualFile();
        if (file == null) return null;
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) return null;
        CargoProject cargoProject = CargoProjectServiceUtil.getCargoProjects(project).findProjectForFile(file);
        if (cargoProject == null) return null;
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain == null) return null;
        Rustfmt rustfmt = Rustfmt.create(toolchain);

        return new FormattingTask() {
            private final ProgressIndicatorBase indicator = new ProgressIndicatorBase();

            @Override
            public void run() {
                RustfmtTestmarks.RustfmtUsed.hit();

                if (Rustup.checkNeedInstallRustfmt(project, CargoCommandConfiguration.getWorkingDirectory(cargoProject))) {
                    request.onTextReady(request.getDocumentText());
                    return;
                }

                GeneralCommandLine commandLine = rustfmt.createCommandLine(cargoProject, document);
                if (commandLine == null) {
                    request.onTextReady(request.getDocumentText());
                    return;
                }

                RsResult<ProcessOutput, ?> result = CommandLineExt.execute(
                    commandLine,
                    project,
                    request.getDocumentText().getBytes(),
                    null
                );

                if (result instanceof RsResult.Ok) {
                    ProcessOutput output = ((RsResult.Ok<ProcessOutput, ?>) result).getOk();
                    request.onTextReady(output.getStdout());
                } else if (result instanceof RsResult.Err) {
                    Object err = ((RsResult.Err<?, ?>) result).getErr();
                    request.onError(RsBundle.message("notification.title.rustfmt"), err.toString());
                }
            }

            @Override
            public boolean cancel() {
                indicator.cancel();
                return true;
            }

            @Override
            public boolean isRunUnderProgress() {
                return true;
            }
        };
    }

    @NotNull
    @Override
    public String getNotificationGroupId() {
        return "Rust Plugin";
    }

    @NotNull
    @Override
    public String getName() {
        return "rustfmt";
    }

    private enum FormattingReason {
        ReformatCode,
        ReformatCodeBeforeCommit,
        Implicit
    }

    @NotNull
    private static FormattingReason getFormattingReason() {
        String currentCommandName = CommandProcessor.getInstance().getCurrentCommandName();
        if (ReformatCodeProcessor.getCommandName().equals(currentCommandName)) {
            return FormattingReason.ReformatCode;
        } else if (FormatterUtil.getReformatBeforeCommitCommandName().equals(currentCommandName)) {
            return FormattingReason.ReformatCodeBeforeCommit;
        } else {
            return FormattingReason.Implicit;
        }
    }
}
