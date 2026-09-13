/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.navigation.ItemPresentation;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.StubElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.presentation.PresentationUtils;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.impl.RsRawIdentifiers;
import org.rust.lang.core.stubs.RsNamedStub;
import org.rust.lang.core.psi.ext.*;

/**
 * Base class for stub-based named Rust PSI elements.
 * Provides name identifier lookup, getName/setName (with stub support),
 * text offset, and presentation.
 */
public abstract class RsStubbedNamedElementImpl<StubT extends StubElement<?> & RsNamedStub>
        extends RsStubbedElementImpl<StubT>
        implements RsNameIdentifierOwner {

    public RsStubbedNamedElementImpl(@Nonnull ASTNode node) {
        super(node);
    }

    public RsStubbedNamedElementImpl(@Nonnull StubT stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return findChildByType(RsElementTypes.IDENTIFIER);
    }

    @Nullable
    @Override
    public String getName() {
        StubT stub = getStub();
        if (stub != null) {
            return stub.getName();
        }
        PsiElement nameId = getNameIdentifier();
        return nameId != null ? RsRawIdentifiers.getUnescapedText(nameId) : null;
    }

    @Override
    public PsiElement setName(@Nonnull String name) {
        PsiElement nameId = getNameIdentifier();
        if (nameId != null) {
            nameId.replace(new RsPsiFactory(getProject()).createIdentifier(name));
        }
        return this;
    }

    @Override
    public int getTextOffset() {
        PsiElement nameId = getNameIdentifier();
        return nameId != null ? nameId.getTextOffset() : super.getTextOffset();
    }

    @Override
    public ItemPresentation getPresentation() {
        return PresentationUtils.getPresentation(this);
    }
}
