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
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.stubs.RsConstantStub;

import javax.swing.*;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

public abstract class RsConstantImplMixin extends RsStubbedNamedElementImpl<RsConstantStub> implements RsConstant {

    public RsConstantImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsConstantImplMixin(@NotNull RsConstantStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    public boolean isMut() {
        return RsConstantUtil.isMut(this);
    }

    public boolean isConst() {
        return RsConstantUtil.isConst(this);
    }

    @NotNull
    public RsConstantKind getKind() {
        return RsConstantUtil.getKind(this);
    }

    @NotNull
    @Override
    public Icon getIcon(int flags) {
        return getIcon(flags, true);
    }

    @NotNull
    public Icon getIcon(int flags, boolean allowNameResolution) {
        RsConstantKind kind = getKind();
        Icon baseIcon;
        switch (kind) {
            case CONST: {
                RsAbstractableOwner owner = allowNameResolution
                    ? RsAbstractableImplUtil.getOwner(this)
                    : RsAbstractableImplUtil.getOwnerBySyntaxOnly(this);
                Icon icon;
                if (owner instanceof RsAbstractableOwner.Trait) {
                    icon = isAbstract() ? RsIcons.ABSTRACT_ASSOC_CONSTANT : RsIcons.ASSOC_CONSTANT;
                } else if (owner instanceof RsAbstractableOwner.Impl) {
                    icon = RsIcons.ASSOC_CONSTANT;
                } else {
                    icon = RsIcons.CONSTANT;
                }
                if (owner.isImplOrTrait() && !owner.isInherentImpl()) return icon;
                baseIcon = icon;
                break;
            }
            case MUT_STATIC:
                baseIcon = RsIcons.MUT_STATIC;
                break;
            case STATIC:
            default:
                baseIcon = RsIcons.STATIC;
                break;
        }
        return RsVisibilityUtil.iconWithVisibility(this, flags, baseIcon);
    }

    public boolean isAbstract() {
        return getExpr() == null;
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

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
