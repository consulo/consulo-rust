/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.codeVision;

import consulo.annotation.component.ExtensionImpl;
import consulo.versionControlSystem.codeVision.VcsCodeVisionCurlyBracketLanguageContext;
import consulo.codeEditor.Editor;
import consulo.application.util.registry.Registry;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.OpenApiUtil;
import consulo.language.Language;
import consulo.ui.event.ComponentEvent;
import org.rust.lang.RsLanguage;


@ExtensionImpl
@SuppressWarnings("UnstableApiUsage")
public class RsVcsCodeVisionContext extends VcsCodeVisionCurlyBracketLanguageContext {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }

    @Override
    public boolean isAccepted(@Nonnull PsiElement element) {
        if (!OpenApiUtil.isUnitTestMode() && !Registry.is("org.rust.code.vision.author", true)) return false;

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
    public boolean isRBrace(@Nonnull PsiElement element) {
        return RsElementUtil.getElementType(element) == RsElementTypes.RBRACE;
    }

    @Override
    public void handleClick(@Nonnull ComponentEvent<?> event, @Nonnull Editor editor, @Nonnull PsiElement element) {
    }
}
