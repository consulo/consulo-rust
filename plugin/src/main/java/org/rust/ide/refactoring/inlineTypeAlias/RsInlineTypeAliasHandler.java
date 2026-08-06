/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.inlineTypeAlias;

import consulo.language.editor.TargetElementUtil;
import consulo.language.Language;
import consulo.language.editor.refactoring.inline.InlineActionHandler;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.openapiext.OpenApiUtil;

public class RsInlineTypeAliasHandler extends InlineActionHandler {

    @Override
    public boolean isEnabledForLanguage(@Nullable Language language) {
        return language == RsLanguage.INSTANCE;
    }

    @Override
    public boolean canInlineElement(@Nonnull PsiElement element) {
        if (!(element instanceof RsTypeAlias)) return false;
        RsTypeAlias typeAlias = (RsTypeAlias) element;
        if (typeAlias.getName() == null) return false;
        if (typeAlias.getTypeParamBounds() != null) return false;
        if (typeAlias.getTypeReference() == null) return false;
        PsiElement parent = typeAlias.getParent();
        if (!(parent instanceof RsMod) && !(parent instanceof RsBlock)) return false;
        if (RsExpandedElementUtil.isExpandedFromMacro(typeAlias)) return false;
        return true;
    }

    @Override
    public void inlineElement(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsTypeAlias typeAlias = (RsTypeAlias) element;
        PsiReference ref = TargetElementUtil.findReference(editor, editor.getCaretModel().getOffset());
        RsReference reference = ref instanceof RsReference ? (RsReference) ref : null;
        if (!org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            RsInlineTypeAliasDialog dialog = new RsInlineTypeAliasDialog(typeAlias, reference);
            dialog.show();
        } else {
            RsInlineTypeAliasProcessor processor = new RsInlineTypeAliasProcessor(project, typeAlias, reference, false);
            processor.run();
        }
    }
}
