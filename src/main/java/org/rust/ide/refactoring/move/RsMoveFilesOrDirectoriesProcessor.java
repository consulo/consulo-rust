/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.refactoring.move.common.ModToMove;
import org.rust.ide.refactoring.move.common.RsModDeclUsageInfo;
import org.rust.ide.refactoring.move.common.RsMoveCommonProcessor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsModUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * See overview of move refactoring in comment for {@link RsMoveCommonProcessor}.
 */
public class RsMoveFilesOrDirectoriesProcessor extends MoveFilesOrDirectoriesProcessor {

    @NotNull
    private final Project project;
    @NotNull
    private final Set<RsFile> filesToMove;
    @NotNull
    private List<ModToMove> elementsToMove;
    @NotNull
    private final RsMoveCommonProcessor commonProcessor;
    @Nullable
    private final MoveCallback moveCallback;

    public RsMoveFilesOrDirectoriesProcessor(
        @NotNull Project project,
        @NotNull PsiElement[] filesOrDirectoriesToMove,
        @NotNull PsiDirectory newParent,
        @NotNull RsMod targetMod,
        @Nullable MoveCallback moveCallback,
        @NotNull Runnable doneCallback
    ) {
        super(project, filesOrDirectoriesToMove, newParent, true, true, true, null, doneCallback);
        this.project = project;
        this.moveCallback = moveCallback;

        this.filesToMove = Arrays.stream(filesOrDirectoriesToMove)
            .map(element -> {
                RsFile file = RsMoveFilesOrDirectoriesHandler.adjustForMove(element);
                if (file == null) {
                    throw new IllegalStateException("File or directory " + element + " can't be moved");
                }
                return file;
            })
            .collect(Collectors.toCollection(LinkedHashSet::new));

        this.elementsToMove = filesToMove.stream().map(ModToMove::new).collect(Collectors.toList());
        this.commonProcessor = new RsMoveCommonProcessor(project, new java.util.ArrayList<>(elementsToMove), targetMod);

        for (RsFile file : filesToMove) {
            String modName = file.getModName();
            if (modName == null) continue;
            if (RsModUtil.getChildModule(targetMod, modName) != null) {
                throw new IncorrectOperationException("Cannot move. Mod with same crate relative path already exists");
            }
        }
    }

    @Override
    @NotNull
    public UsageInfo @NotNull [] findUsages() {
        return commonProcessor.findUsages();
    }

    @Override
    protected boolean preprocessUsages(@NotNull Ref<UsageInfo[]> refUsages) {
        UsageInfo[] usages = refUsages.get();
        MultiMap<PsiElement, String> conflicts = new MultiMap<>();
        checkSingleModDeclaration(usages);
        return commonProcessor.preprocessUsages(usages, conflicts) && showConflicts(conflicts, usages);
    }

    private void checkSingleModDeclaration(@NotNull UsageInfo[] usages) {
        Map<RsFile, List<RsModDeclUsageInfo>> modDeclarationsByFile = Arrays.stream(usages)
            .filter(u -> u instanceof RsModDeclUsageInfo)
            .map(u -> (RsModDeclUsageInfo) u)
            .collect(Collectors.groupingBy(RsModDeclUsageInfo::getFile));

        for (RsFile file : filesToMove) {
            List<RsModDeclUsageInfo> modDeclarations = modDeclarationsByFile.get(file);
            if (modDeclarations == null) {
                throw new IllegalStateException("Can't move " + file.getName() + ".\nIt is not included in module tree");
            }
            if (modDeclarations.size() > 1) {
                throw new IllegalStateException("Can't move " + file.getName() + ".\nIt is declared in more than one parent modules");
            }
        }
    }

    @Override
    protected void performRefactoring(@NotNull UsageInfo @NotNull [] usages) {
        List<RsModDeclUsageInfo> oldModDeclarations = Arrays.stream(usages)
            .filter(u -> u instanceof RsModDeclUsageInfo)
            .map(u -> (RsModDeclUsageInfo) u)
            .collect(Collectors.toList());

        commonProcessor.performRefactoring(usages, () -> {
            moveFilesAndModDeclarations(oldModDeclarations);
            return new java.util.ArrayList<>(elementsToMove);
        });
        if (moveCallback != null) {
            moveCallback.refactoringCompleted();
        }
    }

    private void moveFilesAndModDeclarations(@NotNull List<RsModDeclUsageInfo> oldModDeclarations) {
        moveModDeclaration(oldModDeclarations);
        super.performRefactoring(new UsageInfo[0]);

        for (RsFile file : filesToMove) {
            String crateRelativePath = file.getCrateRelativePath();
            if (crateRelativePath == null || crateRelativePath.isEmpty()) {
                throw new IllegalStateException(
                    file.getName() + " had correct crateRelativePath before moving mod-declaration, but empty/null after move"
                );
            }
        }
    }

    private void moveModDeclaration(@NotNull List<RsModDeclUsageInfo> oldModDeclarationsAll) {
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        Map<RsFile, List<RsModDeclUsageInfo>> grouped = oldModDeclarationsAll.stream()
            .collect(Collectors.groupingBy(RsModDeclUsageInfo::getFile));

        for (Map.Entry<RsFile, List<RsModDeclUsageInfo>> entry : grouped.entrySet()) {
            RsModDeclItem oldModDeclaration = entry.getValue().get(0).getElement();
            commonProcessor.updateMovedItemVisibility(oldModDeclaration);
            PsiElement newModDeclaration = oldModDeclaration.copy();
            oldModDeclaration.delete();
            RsMoveProcessorUtils.insertModDecl((RsMod) commonProcessor.getTargetMod(), psiFactory, newModDeclaration);
        }
    }
}
