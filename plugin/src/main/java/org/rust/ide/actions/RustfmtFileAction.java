/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import consulo.process.ExecutionException;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.tools.Rustfmt;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.OpenApiUtil;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionRef;

@ActionImpl(
    id = "Cargo.RustfmtFile",
    parents = @ActionParentRef(
        value = @ActionRef(id = "CodeMenu"),
        anchor = ActionRefAnchor.LAST
    )
)
public class RustfmtFileAction extends LegacyDumbAwareAction {


    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        e.getPresentation().setEnabled(getContext(e) != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Context ctx = getContext(e);
        if (ctx == null) return;
        OpenApiUtil.checkWriteAccessNotAllowed();
        String formattedText = OpenApiUtil.computeWithCancelableProgress(
            ctx.cargoProject.getProject(),
            RsBundle.message("action.Cargo.RustfmtFile.progress.default.text"),
            () -> reformatDocumentAndGetText(ctx.cargoProject, ctx.rustfmt, ctx.document)
        );
        if (formattedText == null) return;
        String fileName = consulo.document.FileDocumentManager.getInstance().getFile(ctx.document) != null ? consulo.document.FileDocumentManager.getInstance().getFile(ctx.document).getPresentableName() : null;
        String commandName = fileName != null
            ? RsBundle.message("action.Cargo.RustfmtFile.file.text", fileName)
            : RsBundle.message("action.Cargo.RustfmtFile.default.text");
        OpenApiUtil.runWriteCommandAction(ctx.cargoProject.getProject(), commandName, () -> {
            ctx.document.setText(formattedText);
        });
    }

    private String reformatDocumentAndGetText(CargoProject cargoProject, Rustfmt rustfmt, Document document) {
        if (Rustup.checkNeedInstallRustfmt(cargoProject.getProject(), CargoCommandConfiguration.getWorkingDirectory(cargoProject))) return null;
        return rustfmt.reformatDocumentTextOrNull(cargoProject, document);
    }

    private Context getContext(AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return null;
        var toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain == null) return null;
        Rustfmt rustfmt = Rustfmt.rustfmt(toolchain);
        if (rustfmt == null) return null;
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return null;
        Document document = editor.getDocument();
        VirtualFile file = OpenApiUtil.getVirtualFile(document);
        if (file == null) return null;
        if (!(file.isInLocalFileSystem() && RsFile.isRustFile(file))) return null;
        CargoProject cargoProject = CargoProjectServiceUtil.getCargoProjects(project).findProjectForFile(file);
        if (cargoProject == null) return null;
        return new Context(cargoProject, rustfmt, document);
    }

    private record Context(CargoProject cargoProject, Rustfmt rustfmt, Document document) {}
}
