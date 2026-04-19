/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;

import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.psi.ext.RsElementExtUtil;
import org.rust.openapiext.VirtualFileExtUtil;
import org.toml.lang.psi.TomlFileType;

/**
 * Provides navigation from a package Cargo.toml to the workspace Cargo.toml, in addition to
 * {@link org.rust.ide.navigation.goto_.RsGotoSuperHandler}
 */
public class CargoTomlGotoSuperHandler implements LanguageCodeInsightActionHandler {

    @Override
    public boolean isValidFor(@Nullable Editor editor, @Nullable PsiFile file) {
        return Util.tomlPluginIsAbiCompatible() && file != null && file.getFileType() == TomlFileType.INSTANCE;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        NavigatablePsiElement target = gotoSuperTarget(project, file);
        if (target != null) {
            target.navigate(true);
        }
    }

    @Nullable
    private static NavigatablePsiElement gotoSuperTarget(@NotNull Project project, @NotNull PsiFile file) {
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
