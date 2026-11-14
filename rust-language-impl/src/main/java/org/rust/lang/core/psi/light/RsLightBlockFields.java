/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.light;

import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsBlockFields;
import org.rust.lang.core.psi.RsNamedFieldDecl;
import org.rust.lang.core.stubs.RsPlaceholderStub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/** The fields of a {@link RsLightStructItem}: light fields whose parent is the light struct. */
public final class RsLightBlockFields extends RsLightElement<RsBlockFields> implements RsBlockFields {
    @Nonnull
    private final RsLightStructItem owner;
    @Nonnull
    private final List<RsNamedFieldDecl> fields;

    RsLightBlockFields(@Nonnull RsBlockFields anchor, @Nonnull RsLightStructItem owner, @Nonnull Function<PsiElement, PsiElement> fieldOrigins) {
        super(anchor, owner.getOrigin());
        this.owner = owner;
        List<RsNamedFieldDecl> list = new ArrayList<>();
        for (RsNamedFieldDecl field : anchor.getNamedFieldDeclList()) {
            PsiElement origin = fieldOrigins.apply(field);
            list.add(origin != null ? new RsLightNamedFieldDecl(field, this, origin) : field);
        }
        this.fields = Collections.unmodifiableList(list);
    }

    @Override
    public PsiElement getParent() {
        return owner;
    }

    @Override
    public PsiElement getContext() {
        return owner;
    }

    @Nonnull
    @Override
    public List<RsNamedFieldDecl> getNamedFieldDeclList() {
        return fields;
    }

    @Nullable
    @Override
    public PsiElement getLbrace() {
        return anchor.getLbrace();
    }

    @Nullable
    @Override
    public PsiElement getRbrace() {
        return anchor.getRbrace();
    }

    @Nullable
    @Override
    public RsPlaceholderStub<?> getStub() {
        return null;
    }

    @Override
    public IStubElementType getElementType() {
        return anchor.getElementType();
    }
}
