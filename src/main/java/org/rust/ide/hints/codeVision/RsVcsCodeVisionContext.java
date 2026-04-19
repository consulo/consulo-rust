/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.codeVision;

import com.intellij.codeInsight.hints.VcsCodeVisionCurlyBracketLanguageContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.OpenApiUtil;

import java.awt.event.MouseEvent;

@SuppressWarnings("UnstableApiUsage")
public class RsVcsCodeVisionContext extends VcsCodeVisionCurlyBracketLanguageContext {
    @Override
    public boolean isAccepted(@NotNull PsiElement element) {
        if (!OpenApiUtil.isUnitTestMode() && !Registry.is("org.rust.code.vision.author")) return false;

        return element instanceof RsFunction
            || element instanceof RsStructOrEnumItemElement
            || element instanceof RsTraitOrImpl
            || element instanceof RsTypeAlias
            || element instanceof RsConstant
            || element instanceof RsMacroDefinitionBase
            || element instanceof RsModItem
            || element instanceof RsModDeclItem
            || element instanceof RsTraitAlias;
    }

    @Override
    public boolean isRBrace(@NotNull PsiElement element) {
        return RsElementUtil.getElementType(element) == RsElementTypes.RBRACE;
    }

    @Override
    public void handleClick(@NotNull MouseEvent mouseEvent, @NotNull Editor editor, @NotNull PsiElement element) {
    }
}
