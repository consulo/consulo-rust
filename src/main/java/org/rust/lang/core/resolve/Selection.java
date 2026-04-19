/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.infer.Obligation;

import java.util.List;
import java.util.Objects;

import static org.rust.lang.core.types.ExtensionsUtil.emptySubstitution;

public final class Selection {
    @NotNull
    private final RsTraitOrImpl impl;
    @NotNull
    private final List<Obligation> nestedObligations;
    @NotNull
    private final Substitution subst;

    public Selection(@NotNull RsTraitOrImpl impl, @NotNull List<Obligation> nestedObligations) {
        this(impl, nestedObligations, emptySubstitution());
    }

    public Selection(@NotNull RsTraitOrImpl impl, @NotNull List<Obligation> nestedObligations, @NotNull Substitution subst) {
        this.impl = impl;
        this.nestedObligations = nestedObligations;
        this.subst = subst;
    }

    @NotNull
    public RsTraitOrImpl getImpl() {
        return impl;
    }

    @NotNull
    public List<Obligation> getNestedObligations() {
        return nestedObligations;
    }

    @NotNull
    public Substitution getSubst() {
        return subst;
    }

    @NotNull
    public Selection copy(@NotNull List<Obligation> nestedObligations) {
        return new Selection(impl, nestedObligations, subst);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Selection)) return false;
        Selection selection = (Selection) o;
        return impl.equals(selection.impl) &&
            nestedObligations.equals(selection.nestedObligations) &&
            subst.equals(selection.subst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(impl, nestedObligations, subst);
    }

    @Override
    public String toString() {
        return "Selection(impl=" + impl + ", nestedObligations=" + nestedObligations + ", subst=" + subst + ")";
    }
}
