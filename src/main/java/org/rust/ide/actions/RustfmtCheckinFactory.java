/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.CommonBundle;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.changes.CommitContext;
import com.intellij.openapi.vcs.changes.CommitExecutor;
import com.intellij.openapi.vcs.changes.ui.BooleanCommitOption;
import com.intellij.openapi.vcs.checkin.CheckinHandler;
import com.intellij.openapi.vcs.checkin.CheckinHandler.ReturnResult;
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PairConsumer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.tools.Rustfmt;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.ide.settings.RsVcsConfiguration;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;

import java.util.ArrayList;
import java.util.List;

public class RustfmtCheckinFactory extends CheckinHandlerFactory {

    @NotNull
    @Override
    public CheckinHandler createHandler(@NotNull CheckinProjectPanel panel, @NotNull CommitContext commitContext) {
        return new CheckinHandler() {
            @Override
            public RefreshableOnComponent getBeforeCheckinConfigurationPanel() {
                return BooleanCommitOption.create(panel.getProject(), this, false, RsBundle.message("run.rustfmt"),
                    () -> isEnabled(panel),
                    value -> setEnabled(panel, value));
            }

            @Override
            public ReturnResult beforeCheckin(CommitExecutor executor, PairConsumer<Object, Object> additionalDataConsumer) {
                if (!isEnabled(panel)) return ReturnResult.COMMIT;

                FileDocumentManager.getInstance().saveAllDocuments();

                Project project = panel.getProject();
                String[] error = {null};
                String[] errorPath = {null};

                List<FileContext> applicableFiles = new ArrayList<>();
                for (VirtualFile file : panel.getVirtualFiles()) {
                    FileContext ctx = fileContext(file, project);
                    if (ctx != null) applicableFiles.add(ctx);
                }

                for (FileContext ctx : applicableFiles) {
                    String path = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(ctx.document) != null ? com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(ctx.document).getPath() : null;
                    if (Rustup.checkNeedInstallRustfmt(ctx.cargoProject.getProject(), CargoCommandConfiguration.getWorkingDirectory(ctx.cargoProject))) {
                        error[0] = RsBundle.message("rust.checkin.factory.fmt.rustfmt.not.installed.message");
                        errorPath[0] = path;
                        break;
                    }
                    String e;
                    try {
                        RsResult<Void, String> result = execute(ctx.cargoProject, ctx.rustfmt, ctx.document, project);
                        e = result.err();
                    } catch (ProcessCanceledException pce) {
                        return ReturnResult.CANCEL;
                    }

                    if (e != null) {
                        error[0] = e;
                        errorPath[0] = path;
                    }

                    FileDocumentManager.getInstance().saveDocument(ctx.document);
                }

                if (error[0] != null) {
                    return showErrorMessage(panel, executor, error[0], errorPath[0]);
                }

                return ReturnResult.COMMIT;
            }
        };
    }

    private RsResult<Void, String> execute(CargoProject cargoProject, Rustfmt rustfmt, Document document, Project project) {
        String fileName = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(document) != null ? com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getFile(document).getPresentableName() : null;

        String progressMessage = fileName != null
            ? RsBundle.message("action.Cargo.RustfmtFile.progress.file.text", fileName)
            : RsBundle.message("action.Cargo.RustfmtFile.progress.default.text");

        var formattedText = OpenApiUtil.computeWithCancelableProgress(cargoProject.getProject(), progressMessage, () ->
            rustfmt.reformatTextDocument(cargoProject, document, project)
        );
        if (formattedText == null) return new RsResult.Err<>(RsBundle.message("rust.checkin.factory.fmt.failed.message"));

        return formattedText.<Void>map(it -> {
            String commandName = fileName != null
                ? RsBundle.message("action.Cargo.RustfmtFile.file.text", fileName)
                : RsBundle.message("action.Cargo.RustfmtFile.default.text");
            OpenApiUtil.runWriteCommandAction(cargoProject.getProject(), commandName, () -> {
                document.setText(it.getStdout());
            });
            return null;
        }).mapErr(it ->
            it.getMessage() != null ? it.getMessage() : RsBundle.message("rust.checkin.factory.fmt.failed.message")
        );
    }

    private ReturnResult showErrorMessage(CheckinProjectPanel panel, CommitExecutor executor, String errorMsg, String filename) {
        String[] errorLines = errorMsg.split("\n", 2);
        String errorHeader = filename != null
            ? RsBundle.message("rust.checkin.factory.fmt.header.message", filename) + ":<br/>"
            : "";

        String firstLineError;
        String restOfTheError;
        if (errorLines.length == 2) {
            firstLineError = errorLines[0];
            restOfTheError = errorLines[1];
        } else if (errorLines.length == 0) {
            firstLineError = RsBundle.message("rust.checkin.factory.fmt.failed.message");
            restOfTheError = null;
        } else {
            firstLineError = errorLines[0];
            restOfTheError = null;
        }
        String errorDetails = restOfTheError != null
            ? "<br/><br/>" + RsBundle.message("details") + "<br/>" + restOfTheError
            : "";

        String[] buttons = {commitButtonMessage(executor, panel), CommonBundle.getCancelButtonText()};
        String question = RsBundle.message("rust.checkin.factory.fmt.commit.anyway.question");
        String dialogText = RsBundle.message("dialog.message.html.body.br.b.b.body.html", errorHeader, firstLineError != null ? firstLineError : "", question, errorDetails);
        int answer = Messages.showDialog(panel.getProject(), dialogText, RsBundle.message("notification.title.rustfmt"), null, buttons, 0, 1, UIUtil.getWarningIcon());
        return switch (answer) {
            case Messages.OK -> ReturnResult.COMMIT;
            case Messages.NO -> ReturnResult.CLOSE_WINDOW;
            default -> ReturnResult.CANCEL;
        };
    }

    private @NlsContexts.Button String commitButtonMessage(CommitExecutor executor, CheckinProjectPanel panel) {
        return StringUtil.trimEnd(executor != null ? executor.getActionText() : panel.getCommitActionName(), "...");
    }

    private boolean isEnabled(CheckinProjectPanel panel) {
        return RsVcsConfiguration.getInstance(panel.getProject()).getState().getRustFmt();
    }

    private void setEnabled(CheckinProjectPanel panel, boolean enabled) {
        RsVcsConfiguration.getInstance(panel.getProject()).getState().setRustFmt(enabled);
    }

    private FileContext fileContext(VirtualFile file, Project project) {
        var toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain == null) return null;
        Rustfmt rustfmt = Rustfmt.rustfmt(toolchain);
        if (rustfmt == null) return null;
        Document document = OpenApiUtil.getDocument(file);
        if (document == null) return null;
        if (!RsFile.isRustFile(file)) return null;
        CargoProject cargoProject = CargoProjectServiceUtil.getCargoProjects(project).findProjectForFile(file);
        if (cargoProject == null) return null;
        return new FileContext(cargoProject, rustfmt, document);
    }

    private record FileContext(CargoProject cargoProject, Rustfmt rustfmt, Document document) {}
}
