/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMacroBinding;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsPsiImplUtil;

public abstract class RsMacroBindingImplMixin extends RsNamedElementImpl implements RsMacroBinding {

    public RsMacroBindingImplMixin(@NotNull IElementType type) {
        super(type);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return getMetaVarIdentifier();
    }

    @Override
    public PsiElement setName(@NotNull String name) {
        PsiElement nameId = getNameIdentifier();
        if (nameId != null) {
            nameId.replace(new RsPsiFactory(getProject()).createMetavarIdentifier(name));
        }
        return this;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        RsMacroDefinitionBase owner = RsPsiJavaUtil.contextStrict(this, RsMacroDefinitionBase.class);
        if (owner == null) throw new IllegalStateException("Macro binding outside of a macro");
        return RsPsiImplUtil.localOrMacroSearchScope(owner);
    }
}
