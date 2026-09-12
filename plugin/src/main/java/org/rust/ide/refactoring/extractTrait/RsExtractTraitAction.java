/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractTrait;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.dataContext.DataContext;
import consulo.language.Language;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;

@ActionImpl(
    id = "Rust.RsExtractTrait",
    parents = @ActionParentRef(
        value = @ActionRef(id = "IntroduceActionsGroup"),
        anchor = ActionRefAnchor.BEFORE,
        relatedToAction = @ActionRef(id = "ExtractInterface")
    )
)
public class RsExtractTraitAction extends BaseRefactoringAction {

    public RsExtractTraitAction() {
        setInjectedContext(true);
    }

    @Override
    protected boolean isAvailableInEditorOnly() {
        return false;
    }

    @Override
    protected boolean isEnabledOnElements(PsiElement[] elements) {
        return elements.length == 1 && elements[0] instanceof RsTraitOrImpl;
    }

    @Override
    protected boolean isAvailableForLanguage(Language language) {
        return language == RsLanguage.INSTANCE;
    }

    @Nonnull
    @Override
    protected RsExtractTraitHandler getHandler(@Nonnull DataContext dataContext) {
        return new RsExtractTraitHandler();
    }
}
