/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.RsWhereClause;
import org.rust.lang.core.stubs.RsTypeAliasStub;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;

import javax.swing.*;
import java.util.List;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;
import consulo.ui.image.Image;

public abstract class RsTypeAliasImplMixin extends RsStubbedNamedElementImpl<RsTypeAliasStub> implements RsTypeAlias {

    public RsTypeAliasImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsTypeAliasImplMixin(@Nonnull RsTypeAliasStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public consulo.ui.image.Image getIcon(int flags) {
        return getIcon(flags, true);
    }

    @Override
    public consulo.ui.image.Image getIcon(int flags, boolean allowNameResolution) {
        RsAbstractableOwner owner = allowNameResolution
            ? RsAbstractableOwnerUtil.getOwner(this)
            : RsAbstractableImplUtil.getOwnerBySyntaxOnly(this);
        consulo.ui.image.Image baseIcon;
        if (owner == RsAbstractableOwner.Free || owner == RsAbstractableOwner.Foreign) {
            baseIcon = RsIcons.TYPE_ALIAS;
        } else if (owner instanceof RsAbstractableOwner.Trait) {
            baseIcon = isAbstract() ? RsIcons.ABSTRACT_ASSOC_TYPE_ALIAS : RsIcons.ASSOC_TYPE_ALIAS;
        } else {
            baseIcon = RsIcons.ASSOC_TYPE_ALIAS;
        }
        boolean isImplOrTrait = owner.isImplOrTrait();
        boolean isInherentImpl = owner.isInherentImpl();
        if (isImplOrTrait && !isInherentImpl) {
            return baseIcon;
        }
        return RsVisibilityUtil.iconWithVisibility(this, flags, baseIcon);
    }

    @Override
    public boolean isAbstract() {
        return getTypeReference() == null;
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

    @Nonnull
    @Override
    public Ty getDeclaredType() {
        return RsPsiTypeImplUtil.declaredType(this);
    }

    @Nullable
    @Override
    public RsWhereClause getWhereClause() {
        List<RsWhereClause> list = getWhereClauseList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }

    @Nullable
    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
