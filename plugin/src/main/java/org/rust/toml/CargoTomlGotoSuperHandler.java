/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;

import consulo.language.editor.action.LanguageCodeInsightActionHandler;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.psi.ext.RsElementExtUtil;
import org.rust.openapiext.VirtualFileExtUtil;
import org.toml.lang.psi.TomlFileType;

/**
 * Provides navigation from a package Cargo.toml to the workspace Cargo.toml, in addition to
 * {@link org.rust.ide.navigation.goto_.RsGotoSuperHandler}
 */
public class CargoTomlGotoSuperHandler implements LanguageCodeInsightActionHandler {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.toml.lang.TomlLanguage.INSTANCE; }


    @Override
    public boolean isValidFor(@Nullable Editor editor, @Nullable PsiFile file) {
        return Util.tomlPluginIsAbiCompatible() && file != null && file.getFileType() == TomlFileType.INSTANCE;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        NavigatablePsiElement target = gotoSuperTarget(project, file);
        if (target != null) {
            target.navigate(true);
        }
    }

    @Nullable
    private static NavigatablePsiElement gotoSuperTarget(@Nonnull Project project, @Nonnull PsiFile file) {
        if (file.getName().equalsIgnoreCase("cargo.toml")) {
            CargoWorkspace.Package pkg = RsElementExtUtil.findCargoPackage(file);
            if (pkg == null) return null;
            CargoWorkspace workspace = pkg.getWorkspace();
            Object manifestPath = workspace.getManifestPath();
            VirtualFile vf = file.getVirtualFile();
            if (vf == null) return null;
            VirtualFileSystem fs = vf.getFileSystem();
            VirtualFile targetFile = fs.findFileByPath(manifestPath.toString());
            if (targetFile == null) return null;
            PsiFile psiFile = VirtualFileExtUtil.toPsiFile(targetFile, project);
            if (psiFile instanceof NavigatablePsiElement) {
                return (NavigatablePsiElement) psiFile;
            }
            return null;
        }
        return null;
    }
}
