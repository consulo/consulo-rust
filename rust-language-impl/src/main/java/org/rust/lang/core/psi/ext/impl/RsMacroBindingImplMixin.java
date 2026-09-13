/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import consulo.content.scope.SearchScope;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsMacroBinding;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.impl.RsPsiImplUtil;
import org.rust.lang.core.psi.ext.*;

public abstract class RsMacroBindingImplMixin extends RsNamedElementImpl implements RsMacroBinding {

    public RsMacroBindingImplMixin(@Nonnull ASTNode type) {
        super(type);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return getMetaVarIdentifier();
    }

    @Override
    public PsiElement setName(@Nonnull String name) {
        PsiElement nameId = getNameIdentifier();
        if (nameId != null) {
            nameId.replace(new RsPsiFactory(getProject()).createMetavarIdentifier(name));
        }
        return this;
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        RsMacroDefinitionBase owner = RsPsiJavaUtil.contextStrict(this, RsMacroDefinitionBase.class);
        if (owner == null) throw new IllegalStateException("Macro binding outside of a macro");
        return RsPsiImplUtil.localOrMacroSearchScope(owner);
    }
}
