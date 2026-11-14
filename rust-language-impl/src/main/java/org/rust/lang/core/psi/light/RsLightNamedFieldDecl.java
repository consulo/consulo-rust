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
import org.rust.lang.core.psi.RsDefaultParameterValue;
import org.rust.lang.core.psi.RsNamedFieldDecl;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.psi.RsVis;
import org.rust.lang.core.stubs.RsNamedFieldDeclStub;

import java.util.List;
import org.rust.lang.core.psi.impl.RsPsiImplUtil;

/** A field of a {@link RsLightStructItem} that stands for a source field: a field of a protobuf message, for example. */
public final class RsLightNamedFieldDecl extends RsLightElement<RsNamedFieldDecl> implements RsNamedFieldDecl {
    @Nonnull
    private final RsLightBlockFields parent;

    RsLightNamedFieldDecl(@Nonnull RsNamedFieldDecl anchor, @Nonnull RsLightBlockFields parent, @Nonnull PsiElement origin) {
        super(anchor, origin);
        this.parent = parent;
    }

    @Override
    public PsiElement getParent() {
        return parent;
    }

    @Override
    public PsiElement getContext() {
        return parent;
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
    public RsDefaultParameterValue getDefaultParameterValue() {
        return anchor.getDefaultParameterValue();
    }

    @Nonnull
    @Override
    public List<RsOuterAttr> getOuterAttrList() {
        return anchor.getOuterAttrList();
    }

    /** The parsed type: it is resolved in the generated code, where it was written. */
    @Nullable
    @Override
    public RsTypeReference getTypeReference() {
        return anchor.getTypeReference();
    }

    @Nullable
    @Override
    public RsVis getVis() {
        return anchor.getVis();
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return anchor.getColon();
    }

    @Nullable
    @Override
    public PsiElement getIdentifier() {
        return anchor.getIdentifier();
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

    @Nullable
    @Override
    public RsNamedFieldDeclStub getStub() {
        return null;
    }

    @Override
    public IStubElementType getElementType() {
        return anchor.getElementType();
    }
}
