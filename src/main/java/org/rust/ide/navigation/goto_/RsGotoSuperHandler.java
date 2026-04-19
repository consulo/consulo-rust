/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.VirtualFileExtUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsMod;

public class RsGotoSuperHandler implements LanguageCodeInsightActionHandler {

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public boolean isValidFor(@Nullable Editor editor, @Nullable PsiFile file) {
        return file instanceof RsFile;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        PsiElement focusedElement = file.findElementAt(editor.getCaretModel().getOffset());
        if (focusedElement == null) focusedElement = file;
        List<NavigatablePsiElement> targets = gotoSuperTargets(focusedElement);
        if (targets.isEmpty()) {
            return;
        } else if (targets.size() == 1) {
            targets.get(0).navigate(true);
        } else {
            NavigationUtil.getPsiElementPopup(
                targets.toArray(PsiElement.EMPTY_ARRAY),
                RsBundle.message("popup.title.choose.module.declaration")
            ).showInBestPositionFor(editor);
        }
    }

    @NotNull
    public static List<NavigatablePsiElement> gotoSuperTargets(@NotNull PsiElement source) {
        PsiElement item = PsiTreeUtil.getNonStrictParentOfType(
            source,
            RsAbstractable.class,
            RsMod.class
        );

        if (item instanceof RsAbstractable) {
            RsAbstractable abstractable = (RsAbstractable) item;
            RsAbstractableOwner owner = RsAbstractableImplUtil.getOwner(abstractable);
            if (owner.isTraitImpl()) {
                RsAbstractable superItem = RsAbstractableImplUtil.getSuperItem(abstractable);
                if (superItem != null) {
                    return Collections.singletonList(superItem);
                }
                return Collections.emptyList();
            } else {
                return gotoSuperTargets(item.getParent());
            }
        }

        if (item instanceof RsMod) {
            if (item instanceof RsFile) {
                RsFile rsFile = (RsFile) item;
                if (rsFile.isCrateRoot()) {
                    var cargoPkg = RsElementUtil.getContainingCargoPackage(rsFile);
                    if (cargoPkg == null) return Collections.emptyList();
                    Path manifestPath = cargoPkg.getRootDirectory().resolve("Cargo.toml");
                    var vf = rsFile.getVirtualFile();
                    if (vf == null) return Collections.emptyList();
                    var fs = vf.getFileSystem();
                    var manifestVf = fs.findFileByPath(manifestPath.toString());
                    if (manifestVf == null) return Collections.emptyList();
                    var psiFile = VirtualFileExtUtil.toPsiFile(manifestVf, rsFile.getProject());
                    if (psiFile instanceof NavigatablePsiElement) {
                        return Collections.singletonList((NavigatablePsiElement) psiFile);
                    }
                    return Collections.emptyList();
                } else {
                    List<org.rust.lang.core.psi.RsModDeclItem> declarations = rsFile.getDeclarations();
                    return new ArrayList<>(declarations);
                }
            } else {
                RsMod superMod = RsModExtUtil.getSuper((RsMod) item);
                if (superMod != null) {
                    return Collections.singletonList(superMod);
                }
                return Collections.emptyList();
            }
        }

        return Collections.emptyList();
    }
}
