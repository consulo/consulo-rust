/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.util.CachedValueImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.ide.presentation.PresentationUtils;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.RsImplItemStub;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.resolve.RsCachedImplItem;

public abstract class RsImplItemImplMixin extends RsStubbedElementImpl<RsImplItemStub> implements RsImplItem {

    public RsImplItemImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsImplItemImplMixin(@NotNull RsImplItemStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Override
    public Icon getIcon(int flags) {
        return RsIcons.IMPL;
    }

    @Override
    public boolean isPublic() {
        return false;
    }

    @Override
    public ItemPresentation getPresentation() {
        return PresentationUtils.getPresentation(this);
    }

    @Override
    public int getTextOffset() {
        return getTypeReference() != null ? getTypeReference().getTextOffset() : getImpl().getTextOffset();
    }

    @Nullable
    @Override
    public BoundElement<RsTraitItem> getImplementedTrait() {
        RsTraitRef traitRef = getTraitRef();
        if (traitRef == null) return null;
        BoundElement<RsTraitItem> bound = RsTraitRefUtil.resolveToBoundTrait(traitRef);
        if (bound == null) return null;
        return new BoundElement<RsTraitItem>(bound.element(), bound.getSubst());
    }

    @NotNull
    @Override
    public Collection<RsTypeAlias> getAssociatedTypesTransitively() {
        return CachedValuesManager.getCachedValue(this, () ->
            CachedValueProvider.Result.create(
                doGetAssociatedTypesTransitively(),
                RsPsiUtilUtil.getRustStructureOrAnyPsiModificationTracker(this)
            )
        );
    }

    @NotNull
    private List<RsTypeAlias> doGetAssociatedTypesTransitively() {
        List<RsTypeAlias> implAliases = RsMembersUtil.getTypes(RsMembersUtil.getExpandedMembers(this));
        BoundElement<RsTraitItem> trait = getImplementedTrait();
        if (trait == null) return implAliases;
        Collection<RsTypeAlias> traitAliases = RsTraitItemUtil.getAssociatedTypesTransitively(trait);
        List<RsTypeAlias> result = new ArrayList<>(implAliases);
        for (RsTypeAlias trAl : traitAliases) {
            boolean found = false;
            for (RsTypeAlias implAl : implAliases) {
                if (implAl.getName() != null && implAl.getName().equals(trAl.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) result.add(trAl);
        }
        return result;
    }

    @NotNull
    @Override
    public Ty getDeclaredType() {
        return RsPsiTypeImplUtil.declaredType(this);
    }

    @Override
    public boolean isUnsafe() {
        return getUnsafe() != null;
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }

    @NotNull
    public CachedValue<RsCachedImplItem> getCachedImplItem() {
        return new CachedValueImpl<>(() -> {
            RsCachedImplItem cachedImpl = new RsCachedImplItem(this);
            return RsCachedImplItem.toCachedResult(this, getContainingCrate(), cachedImpl);
        });
    }
}
