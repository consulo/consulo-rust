/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.presentation.PresentationUtils;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsRawIdentifiers;
import org.rust.lang.core.stubs.RsNamedStub;

/**
 * Base class for stub-based named Rust PSI elements.
 * Provides name identifier lookup, getName/setName (with stub support),
 * text offset, and presentation.
 */
public abstract class RsStubbedNamedElementImpl<StubT extends StubElement<?> & RsNamedStub>
        extends RsStubbedElementImpl<StubT>
        implements RsNameIdentifierOwner {

    public RsStubbedNamedElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    public RsStubbedNamedElementImpl(@NotNull StubT stub, @NotNull IStubElementType<?, ?> nodeType) {
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
    public PsiElement setName(@NotNull String name) {
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
