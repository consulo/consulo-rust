/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.light;

import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsBlockFields;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.RsSuperStructs;
import org.rust.lang.core.psi.RsTupleFields;
import org.rust.lang.core.psi.RsTypeParameterList;
import org.rust.lang.core.psi.RsVis;
import org.rust.lang.core.psi.RsWhereClause;
import org.rust.lang.core.stubs.RsStructItemStub;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;
import java.util.function.Function;
import org.rust.lang.core.psi.impl.RsPsiImplUtil;

/** A generated struct that stands for a source element: a protobuf message, for example. */
public final class RsLightStructItem extends RsLightElement<RsStructItem> implements RsStructItem {
    @Nullable
    private final RsLightBlockFields blockFields;

    RsLightStructItem(@Nonnull RsStructItem anchor, @Nonnull PsiElement origin, @Nonnull Function<PsiElement, PsiElement> fieldOrigins) {
        super(anchor, origin);
        RsBlockFields fields = anchor.getBlockFields();
        this.blockFields = fields != null ? new RsLightBlockFields(fields, this, fieldOrigins) : null;
    }

    @Override
    public String getName() {
        return anchor.getName();
    }

    @Override
    public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
        return renameOrigin(name);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return anchor.getNameIdentifier();
    }

    @Nullable
    @Override
    public RsBlockFields getBlockFields() {
        return blockFields;
    }

    @Nullable
    @Override
    public RsSuperStructs getSuperStructs() {
        return anchor.getSuperStructs();
    }

    @Nullable
    @Override
    public RsTupleFields getTupleFields() {
        return anchor.getTupleFields();
    }

    @Nullable
    @Override
    public RsTypeParameterList getTypeParameterList() {
        return anchor.getTypeParameterList();
    }

    @Nullable
    @Override
    public RsWhereClause getWhereClause() {
        return anchor.getWhereClause();
    }

    @Nullable
    @Override
    public PsiElement getSemicolon() {
        return anchor.getSemicolon();
    }

    @Nullable
    @Override
    public PsiElement getIdentifier() {
        return anchor.getIdentifier();
    }

    @Nullable
    @Override
    public PsiElement getStruct() {
        return anchor.getStruct();
    }

    @Nullable
    @Override
    public RsVis getVis() {
        return anchor.getVis();
    }

    @Nonnull
    @Override
    public List<RsOuterAttr> getOuterAttrList() {
        return anchor.getOuterAttrList();
    }

    @Nonnull
    @Override
    public Ty getDeclaredType() {
        return RsPsiTypeImplUtil.declaredType(this);
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

    @Nullable
    @Override
    public RsStructItemStub getStub() {
        return null;
    }

    @Override
    public IStubElementType getElementType() {
        return anchor.getElementType();
    }
}
