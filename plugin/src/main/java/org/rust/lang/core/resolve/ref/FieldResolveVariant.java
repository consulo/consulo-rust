/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve.ScopeEntry;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.infer.Autoderef;
import org.rust.lang.core.types.infer.Obligation;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class FieldResolveVariant implements DotExprResolveVariant {
    @Nonnull
    private final String myName;
    @Nonnull
    private final RsElement myElement;
    @Nonnull
    private final Ty mySelfTy;
    @Nonnull
    private final List<Autoderef.AutoderefStep> myDerefSteps;
    @Nonnull
    private final List<Obligation> myObligations;

    public FieldResolveVariant(
        @Nonnull String name,
        @Nonnull RsElement element,
        @Nonnull Ty selfTy,
        @Nonnull List<Autoderef.AutoderefStep> derefSteps,
        @Nonnull List<Obligation> obligations
    ) {
        myName = name;
        myElement = element;
        mySelfTy = selfTy;
        myDerefSteps = derefSteps;
        myObligations = obligations;
    }

    @Nonnull
    @Override
    public String getName() {
        return myName;
    }

    @Nonnull
    @Override
    public RsElement getElement() {
        return myElement;
    }

    @Nonnull
    @Override
    public Ty getSelfTy() {
        return mySelfTy;
    }

    @Nonnull
    public List<Autoderef.AutoderefStep> getDerefSteps() {
        return myDerefSteps;
    }

    @Nonnull
    public List<Obligation> getObligations() {
        return myObligations;
    }

    @Override
    public int getDerefCount() {
        return myDerefSteps.size();
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
        FieldResolveVariant that = (FieldResolveVariant) o;
        return Objects.equals(myName, that.myName) &&
            Objects.equals(myElement, that.myElement) &&
            Objects.equals(mySelfTy, that.mySelfTy) &&
            Objects.equals(myDerefSteps, that.myDerefSteps) &&
            Objects.equals(myObligations, that.myObligations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myName, myElement, mySelfTy, myDerefSteps, myObligations);
    }
}
