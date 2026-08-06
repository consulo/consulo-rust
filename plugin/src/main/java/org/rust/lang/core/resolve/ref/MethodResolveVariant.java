/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ty.Ty;

import java.util.Objects;
import java.util.Set;

public class MethodResolveVariant implements DotExprResolveVariant, AssocItemScopeEntryBase<RsFunction> {
    @Nonnull
    private final String myName;
    @Nonnull
    private final RsFunction myElement;
    @Nonnull
    private final Ty mySelfTy;
    private final int myDerefCount;
    @Nonnull
    private final TraitImplSource mySource;

    public MethodResolveVariant(
        @Nonnull String name,
        @Nonnull RsFunction element,
        @Nonnull Ty selfTy,
        int derefCount,
        @Nonnull TraitImplSource source
    ) {
        myName = name;
        myElement = element;
        mySelfTy = selfTy;
        myDerefCount = derefCount;
        mySource = source;
    }

    @Nonnull
    @Override
    public String getName() {
        return myName;
    }

    @Nonnull
    @Override
    public RsFunction getElement() {
        return myElement;
    }

    @Nonnull
    @Override
    public Ty getSelfTy() {
        return mySelfTy;
    }

    @Override
    public int getDerefCount() {
        return myDerefCount;
    }

    @Nonnull
    @Override
    public TraitImplSource getSource() {
        return mySource;
    }

    @Nonnull
    @Override
    public Substitution getSubst() {
        return Substitution.getEMPTY();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodResolveVariant that = (MethodResolveVariant) o;
        return myDerefCount == that.myDerefCount &&
            Objects.equals(myName, that.myName) &&
            Objects.equals(myElement, that.myElement) &&
            Objects.equals(mySelfTy, that.mySelfTy) &&
            Objects.equals(mySource, that.mySource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myName, myElement, mySelfTy, myDerefCount, mySource);
    }

    @Nonnull
    @Override
    public ScopeEntry copyWithNs(@Nonnull Set<Namespace> namespaces) {
        return this;
    }
}
