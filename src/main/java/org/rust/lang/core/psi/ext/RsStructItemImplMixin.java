/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.stubs.RsStructItemStub;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;

import javax.swing.*;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

public abstract class RsStructItemImplMixin extends RsStubbedNamedElementImpl<RsStructItemStub> implements RsStructItem {

    public RsStructItemImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsStructItemImplMixin(@NotNull RsStructItemStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Override
    public Icon getIcon(int flags) {
        RsStructKind kind = RsStructItemUtil.getKind(this);
        Icon baseIcon;
        switch (kind) {
            case STRUCT:
                baseIcon = RsIcons.STRUCT;
                break;
            case UNION:
                baseIcon = RsIcons.UNION;
                break;
            default:
                baseIcon = RsIcons.STRUCT;
                break;
        }
        return RsVisibilityUtil.iconWithVisibility(this, flags, baseIcon);
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

    @NotNull
    @Override
    public Ty getDeclaredType() {
        return RsPsiTypeImplUtil.declaredType(this);
    }

    @Nullable
    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
