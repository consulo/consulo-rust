/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsPromoteModuleToDirectoryAction;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsModDeclItem;

import java.util.List;

public class AddModuleFileFix extends RsQuickFixBase<RsModDeclItem> {
    @IntentionName
    private final String text;
    private final boolean expandModuleFirst;
    private final Location location;

    public enum Location {
        File,
        Directory
    }

    public AddModuleFileFix(@NotNull RsModDeclItem modDecl, boolean expandModuleFirst) {
        this(modDecl, expandModuleFirst, Location.File);
    }

    public AddModuleFileFix(@NotNull RsModDeclItem modDecl, boolean expandModuleFirst, @NotNull Location location) {
        super(modDecl);
        this.expandModuleFirst = expandModuleFirst;
        this.location = location;
        this.text = RsBundle.message("intention.name.create.module.file", getPath(modDecl));
    }

    private String getPath(@NotNull RsModDeclItem modDecl) {
        switch (location) {
            case File: return modDecl.getName() + ".rs";
            case Directory: return modDecl.getName() + "/mod.rs";
            default: return "";
        }
    }

    @NotNull
    @Override
    public String getText() {
        return text;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.create.module.file");
    }

    @Nullable
    @Override
    public FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
        return null;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsModDeclItem element) {
        if (expandModuleFirst) {
            RsFile containingFile = (RsFile) element.getContainingFile();
            RsPromoteModuleToDirectoryAction.expandModule(containingFile);
        }

        var existing = element.getReference().resolve();
        if (existing != null) {
            existing.getContainingFile().navigate(true);
            return;
        }

        PsiDirectory dir = element.getContainingMod().getOwnedDirectory(true);
        if (dir == null) return;

        PsiFile file;
        switch (location) {
            case File:
                file = dir.createFile(element.getName() + ".rs");
                break;
            case Directory:
                PsiDirectory subDir = dir.findSubdirectory(element.getName());
                if (subDir == null) subDir = dir.createSubdirectory(element.getName());
                file = subDir.createFile(RsConstants.MOD_RS_FILE);
                break;
            default:
                return;
        }
        file.navigate(true);
    }

    @NotNull
    public static List<AddModuleFileFix> createFixes(@NotNull RsModDeclItem modDecl, boolean expandModuleFirst) {
        return List.of(
            new AddModuleFileFix(modDecl, expandModuleFirst, Location.File),
            new AddModuleFileFix(modDecl, expandModuleFirst, Location.Directory)
        );
    }
}
