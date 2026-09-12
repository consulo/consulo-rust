/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import consulo.language.editor.intention.BaseIntentionAction;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.psi.PsiElement;
import consulo.language.file.light.LightVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import org.rust.cargo.project.model.CargoProjectsUtil;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.List;
import consulo.language.psi.PsiFile;
import consulo.document.util.TextRange;

public final class PsiModificationUtil {
    public static final PsiModificationUtil INSTANCE = new PsiModificationUtil();

    private PsiModificationUtil() {
    }

    public static boolean canReplaceAll(PsiElement... elements) {
        for (PsiElement element : elements) {
            if (!canReplace(element)) return false;
        }
        return true;
    }

    public static boolean canReplaceAll(List<PsiElement> elements) {
        for (PsiElement element : elements) {
            if (!canReplace(element)) return false;
        }
        return true;
    }

    public static boolean canReplace(PsiElement element) {
        PsiElement macroCall = RsExpandedElementUtil.findMacroCallExpandedFrom(element);
        if (macroCall == null) {
            return isWriteableRegardlessMacros(element);
        } else {
            consulo.document.util.TextRange sourceRange = RsExpandedElementUtil.mapRangeFromExpansionToCallBodyStrict(element, element.getTextRange());
            return sourceRange != null && isWriteableRegardlessMacros(macroCall);
        }
    }

    public static boolean isWriteableRegardlessMacros(PsiElement element) {
        OpenApiUtil.testAssert(() -> !RsExpandedElementUtil.isExpandedFromMacro(element));

        if (!BaseIntentionAction.canModify(element)) return false;

        consulo.language.psi.PsiFile containingFile = element.getContainingFile();
        VirtualFile virtualFile = containingFile.getVirtualFile();
        if (virtualFile == null) return true;

        VirtualFile resolvedFile;
        if (virtualFile instanceof LightVirtualFile) {
            resolvedFile = ((LightVirtualFile) virtualFile).getOriginalFile();
        } else if (virtualFile instanceof VirtualFileWindow) {
            resolvedFile = ((VirtualFileWindow) virtualFile).getDelegate();
        } else {
            resolvedFile = virtualFile;
        }
        if (resolvedFile == null) return true;

        if (CargoProjectsUtil.isGeneratedFile(containingFile.getProject(), resolvedFile)) return false;

        return true;
    }
}
