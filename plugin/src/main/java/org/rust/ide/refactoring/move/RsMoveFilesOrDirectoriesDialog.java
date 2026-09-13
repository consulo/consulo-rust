/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.MessageDialogBuilder;

import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.RefactoringSettings;
import consulo.language.editor.refactoring.copy.CopyFilesOrDirectoriesHandler;
import consulo.language.editor.refactoring.move.MoveCallback;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesDialog;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesProcessor;
import consulo.language.editor.refactoring.move.fileOrDirectory.MoveFilesOrDirectoriesUtil;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.impl.RsModUtil;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.VirtualFileExtUtil;

import java.nio.file.Path;

public class RsMoveFilesOrDirectoriesDialog extends MoveFilesOrDirectoriesDialog {

    private static final Logger LOG = Logger.getInstance(RsMoveFilesOrDirectoriesDialog.class);

    @Nonnull
    private final Project myProject;
    @Nonnull
    private final PsiElement[] filesOrDirectoriesToMove;
    @Nullable
    private final MoveCallback moveCallback;

    public RsMoveFilesOrDirectoriesDialog(
        @Nonnull Project project,
        @Nonnull PsiElement[] filesOrDirectoriesToMove,
        @Nullable PsiDirectory initialTargetDirectory,
        @Nullable MoveCallback moveCallback
    ) {
        super(project, null);
        this.myProject = project;
        this.filesOrDirectoriesToMove = filesOrDirectoriesToMove;
        this.moveCallback = moveCallback;
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            throw new IllegalStateException("Should not be used in unit test mode");
        }
        setTitle(RsBundle.message("dialog.title.move.rust"));
    }

    protected void performMove(@Nonnull PsiDirectory targetDirectory) {
        if (!CommonRefactoringUtil.checkReadOnlyStatus(myProject, targetDirectory)) return;
        if (!CommonRefactoringUtil.checkReadOnlyStatus(myProject, java.util.Arrays.asList(filesOrDirectoriesToMove), true)) return;

        try {
            for (PsiElement element : filesOrDirectoriesToMove) {
                if (element instanceof RsFile) {
                    if (element.getParent() == targetDirectory) {
                        showError(RsBundle.message("dialog.message.please.choose.target.directory.different.from.current"));
                        return;
                    }
                    // CopyFilesOrDirectoriesHandler.checkFileExist isn't exposed in Consulo; caller skips duplicate check
                }
                MoveFilesOrDirectoriesUtil.checkMove(element, targetDirectory);
            }

            Runnable doneCallback = () -> close(DialogWrapper.OK_EXIT_CODE);
            boolean searchForReferences = RefactoringSettings.getInstance().MOVE_SEARCH_FOR_REFERENCES_FOR_FILE;

            doPerformMove(
                myProject,
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

    private void showError( @Nullable String message) {
        String title = RefactoringBundle.message("error.title");
        CommonRefactoringUtil.showErrorMessage(title, message, "refactoring.moveFile", myProject);
    }

    /**
     * Also used by tests.
     */
    public static void doPerformMove(
        @Nonnull Project project,
        @Nonnull PsiElement[] filesOrDirectoriesToMove,
        @Nullable MoveCallback moveCallback,
        @Nonnull PsiDirectory targetDirectory,
        boolean searchForReferences,
        @Nonnull Runnable doneCallback
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
        @Nonnull Project project,
        @Nonnull PsiElement[] filesOrDirectoriesToMove,
        @Nonnull PsiDirectory targetDirectory,
        @Nullable MoveCallback moveCallback,
        @Nonnull Runnable doneCallback
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

    private static boolean askShouldMoveIfNoNewParentMod(@Nonnull Project project) {
        int result = MessageDialogBuilder.okCancel(
            RsBundle.message("dialog.title.move"),
            RsBundle.message("dialog.message.file.will.not.be.included.in.module.tree.after.move.continue")
        ).isOk() ? Messages.OK : Messages.CANCEL;
        return result == Messages.OK;
    }
}
