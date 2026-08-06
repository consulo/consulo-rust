/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsPromoteModuleToDirectoryAction;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsModDeclItem;

import java.util.List;

public class AddModuleFileFix extends RsQuickFixBase<RsModDeclItem> {
    
    private final String text;
    private final boolean expandModuleFirst;
    private final Location location;

    public enum Location {
        File,
        Directory
    }

    public AddModuleFileFix(@Nonnull RsModDeclItem modDecl, boolean expandModuleFirst) {
        this(modDecl, expandModuleFirst, Location.File);
    }

    public AddModuleFileFix(@Nonnull RsModDeclItem modDecl, boolean expandModuleFirst, @Nonnull Location location) {
        super(modDecl);
        this.expandModuleFirst = expandModuleFirst;
        this.location = location;
        this.text = RsBundle.message("intention.name.create.module.file", getPath(modDecl));
    }

    private String getPath(@Nonnull RsModDeclItem modDecl) {
        switch (location) {
            case File: return modDecl.getName() + ".rs";
            case Directory: return modDecl.getName() + "/mod.rs";
            default: return "";
        }
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(text);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.create.module.file"));
    }

    @Nullable
    @Override
    public FileModifier getFileModifierForPreview(@Nonnull PsiFile target) {
        return null;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsModDeclItem element) {
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

    @Nonnull
    public static List<AddModuleFileFix> createFixes(@Nonnull RsModDeclItem modDecl, boolean expandModuleFirst) {
        return List.of(
            new AddModuleFileFix(modDecl, expandModuleFirst, Location.File),
            new AddModuleFileFix(modDecl, expandModuleFirst, Location.Directory)
        );
    }
}
