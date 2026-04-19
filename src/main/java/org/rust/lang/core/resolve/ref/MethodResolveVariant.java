/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ty.Ty;

import java.util.Objects;
import java.util.Set;

public class MethodResolveVariant implements DotExprResolveVariant, AssocItemScopeEntryBase<RsFunction> {
    @NotNull
    private final String myName;
    @NotNull
    private final RsFunction myElement;
    @NotNull
    private final Ty mySelfTy;
    private final int myDerefCount;
    @NotNull
    private final TraitImplSource mySource;

    public MethodResolveVariant(
        @NotNull String name,
        @NotNull RsFunction element,
        @NotNull Ty selfTy,
        int derefCount,
        @NotNull TraitImplSource source
    ) {
        myName = name;
        myElement = element;
        mySelfTy = selfTy;
        myDerefCount = derefCount;
        mySource = source;
    }

    @NotNull
    @Override
    public String getName() {
        return myName;
    }

    @NotNull
    @Override
    public RsFunction getElement() {
        return myElement;
    }

    @NotNull
    @Override
    public Ty getSelfTy() {
        return mySelfTy;
    }

    @Override
    public int getDerefCount() {
        return myDerefCount;
    }

    @NotNull
    @Override
    public TraitImplSource getSource() {
        return mySource;
    }

    @NotNull
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

    @NotNull
    @Override
    public ScopeEntry copyWithNs(@NotNull Set<Namespace> namespaces) {
        return this;
    }
}
