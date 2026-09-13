/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import org.rust.cargo.toolchain.RsToolchainLocator;
import consulo.ide.impl.idea.codeInsight.actions.ReformatCodeProcessor;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.ProcessOutput;
import consulo.language.codeStyle.FormattingContext;
import com.intellij.formatting.service.AsyncDocumentFormattingService;
import com.intellij.formatting.service.AsyncFormattingRequest;
import com.intellij.formatting.service.FormattingService;
import consulo.undoRedo.CommandProcessor;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.language.codeStyle.FormatterUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.ide.rustfmt.Rustfmt;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.openapiext.CommandLineExt;
import org.rust.stdext.RsResult;

import java.util.Collections;
import java.util.Set;

public class RustfmtFormattingService extends AsyncDocumentFormattingService {

    private static final Set<FormattingService.Feature> FEATURES = Collections.emptySet();

    @Nonnull
    @Override
    public Set<Feature> getFeatures() {
        return FEATURES;
    }

    @Override
    public boolean canFormat(@Nonnull PsiFile file) {
        return file instanceof RsFile
            && RsProjectSettingsServiceUtil.getRustfmtSettings(file.getProject()).getUseRustfmt()
            && getFormattingReason() == FormattingReason.ReformatCode;
    }

    @Nullable
    @Override
    protected FormattingTask createFormattingTask(@Nonnull AsyncFormattingRequest request) {
        FormattingContext context = request.getContext();
        Project project = context.getProject();
        VirtualFile file = context.getContainingFile() != null ? context.getContainingFile().getVirtualFile() : null;
        if (file == null) return null;
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document == null) return null;
        CargoProject cargoProject = CargoProjectServiceUtil.getCargoProjects(project).findProjectForFile(file);
        if (cargoProject == null) return null;
        RsToolchainBase toolchain = RsToolchainLocator.getToolchain(project);
        if (toolchain == null) return null;
        Rustfmt rustfmt = Rustfmt.create(toolchain);

        return new FormattingTask() {
            private final EmptyProgressIndicator indicator = new EmptyProgressIndicator();

            @Override
            public void run() {
                RustfmtTestmarks.RustfmtUsed.hit();

                if (Rustup.checkNeedInstallRustfmt(project, org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(cargoProject))) {
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

    @Nonnull
    @Override
    public String getNotificationGroupId() {
        return "Rust Plugin";
    }

    @Nonnull
    @Override
    public String getName() {
        return "rustfmt";
    }

    @Nonnull
    @Override
    public com.intellij.formatting.service.AsyncDocumentFormattingService.FormattingReason getFormattingReason() {
        return com.intellij.formatting.service.AsyncDocumentFormattingService.FormattingReason.ReformatCode;
    }
}
