/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesDialog;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsModUtil;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.VirtualFileExtUtil;

import java.nio.file.Path;

public class RsMoveFilesOrDirectoriesDialog extends MoveFilesOrDirectoriesDialog {

    private static final Logger LOG = Logger.getInstance(RsMoveFilesOrDirectoriesDialog.class);

    @NotNull
    private final PsiElement[] filesOrDirectoriesToMove;
    @Nullable
    private final MoveCallback moveCallback;

    public RsMoveFilesOrDirectoriesDialog(
        @NotNull Project project,
        @NotNull PsiElement[] filesOrDirectoriesToMove,
        @Nullable PsiDirectory initialTargetDirectory,
        @Nullable MoveCallback moveCallback
    ) {
        super(project, filesOrDirectoriesToMove, initialTargetDirectory);
        this.filesOrDirectoriesToMove = filesOrDirectoriesToMove;
        this.moveCallback = moveCallback;
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            throw new IllegalStateException("Should not be used in unit test mode");
        }
        setTitle(RsBundle.message("dialog.title.move.rust"));
    }

    @Override
    protected void performMove(@NotNull PsiDirectory targetDirectory) {
        if (!CommonRefactoringUtil.checkReadOnlyStatus(getProject(), targetDirectory)) return;
        if (!CommonRefactoringUtil.checkReadOnlyStatus(getProject(), java.util.Arrays.asList(filesOrDirectoriesToMove), true)) return;

        try {
            for (PsiElement element : filesOrDirectoriesToMove) {
                if (element instanceof RsFile) {
                    if (element.getParent() == targetDirectory) {
                        showError(RsBundle.message("dialog.message.please.choose.target.directory.different.from.current"));
                        return;
                    }
                    CopyFilesOrDirectoriesHandler.checkFileExist(targetDirectory, null, (com.intellij.psi.PsiFile) element, ((RsFile) element).getName(), RsBundle.message("dialog.title.move"));
                }
                MoveFilesOrDirectoriesUtil.checkMove(element, targetDirectory);
            }

            Runnable doneCallback = () -> close(DialogWrapper.OK_EXIT_CODE);
            boolean searchForReferences = RefactoringSettings.getInstance().MOVE_SEARCH_FOR_REFERENCES_FOR_FILE;

            doPerformMove(
                getProject(),
                filesOrDirectoriesToMove,
                moveCallback,
                targetDirectory,
                searchForReferences,
                doneCallback
            );
        } catch (Exception e) {
            if (!(e instanceof IncorrectOperationException)) {
                LOG.error(e);
            }
            showError(e.getMessage());
        }
    }

    private void showError(@DialogMessage @Nullable String message) {
        String title = RefactoringBundle.message("error.title");
        CommonRefactoringUtil.showErrorMessage(title, message, "refactoring.moveFile", getProject());
    }

    /**
     * Also used by tests.
     */
    public static void doPerformMove(
        @NotNull Project project,
        @NotNull PsiElement[] filesOrDirectoriesToMove,
        @Nullable MoveCallback moveCallback,
        @NotNull PsiDirectory targetDirectory,
        boolean searchForReferences,
        @NotNull Runnable doneCallback
    ) {
        if (!searchForReferences) {
            runDefaultProcessor(project, filesOrDirectoriesToMove, targetDirectory, moveCallback, doneCallback);
            return;
        }

        RsFile firstFile = RsMoveFilesOrDirectoriesHandler.adjustForMove(filesOrDirectoriesToMove[0]);
        if (firstFile == null) {
            throw new IllegalStateException("One of moved file is not included in module tree");
        }
        RsMod crateRoot = firstFile.getCrateRoot();
        if (crateRoot == null) {
            throw new IllegalStateException("One of moved file is not included in module tree");
        }
        RsMod targetMod = RsMoveDirectoryUtils.getOwningMod(targetDirectory, crateRoot);
        if (targetMod == null) {
            if (askShouldMoveIfNoNewParentMod(project)) {
                runDefaultProcessor(project, filesOrDirectoriesToMove, targetDirectory, moveCallback, doneCallback);
            }
            return;
        }

        new RsMoveFilesOrDirectoriesProcessor(
            project,
            filesOrDirectoriesToMove,
            targetDirectory,
            targetMod,
            moveCallback,
            doneCallback
        ).run();
    }

    private static void runDefaultProcessor(
        @NotNull Project project,
        @NotNull PsiElement[] filesOrDirectoriesToMove,
        @NotNull PsiDirectory targetDirectory,
        @Nullable MoveCallback moveCallback,
        @NotNull Runnable doneCallback
    ) {
        new MoveFilesOrDirectoriesProcessor(
            project,
            filesOrDirectoriesToMove,
            targetDirectory,
            false,
            true,
            true,
            moveCallback,
            doneCallback
        ).run();
    }

    private static boolean askShouldMoveIfNoNewParentMod(@NotNull Project project) {
        int result = MessageDialogBuilder.okCancel(
            RsBundle.message("dialog.title.move"),
            RsBundle.message("dialog.message.file.will.not.be.included.in.module.tree.after.move.continue")
        ).ask(project) ? Messages.OK : Messages.CANCEL;
        return result == Messages.OK;
    }
}
