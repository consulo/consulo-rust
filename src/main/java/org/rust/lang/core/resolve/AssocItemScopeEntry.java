/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ty.Ty;

import java.util.Objects;
import java.util.Set;

public final class AssocItemScopeEntry implements ScopeEntry {
    @NotNull
    private final String name;
    @NotNull
    private final RsAbstractable element;
    @NotNull
    private final Set<Namespace> namespaces;
    @NotNull
    private final Substitution subst;
    @NotNull
    private final Ty selfTy;
    @NotNull
    private final TraitImplSource source;

    public AssocItemScopeEntry(@NotNull String name, @NotNull RsAbstractable element,
                                @NotNull Set<Namespace> namespaces, @NotNull Substitution subst,
                                @NotNull Ty selfTy, @NotNull TraitImplSource source) {
        this.name = name;
        this.element = element;
        this.namespaces = namespaces;
        this.subst = subst;
        this.selfTy = selfTy;
        this.source = source;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public RsAbstractable getElement() {
        return element;
    }

    @NotNull
    @Override
    public Set<Namespace> getNamespaces() {
        return namespaces;
    }

    @NotNull
    @Override
    public Substitution getSubst() {
        return subst;
    }

    @NotNull
    public Ty getSelfTy() {
        return selfTy;
    }

    @NotNull
    public TraitImplSource getSource() {
        return source;
    }

    @NotNull
    @Override
    public ScopeEntry copyWithNs(@NotNull Set<Namespace> namespaces) {
        return new AssocItemScopeEntry(name, element, namespaces, subst, selfTy, source);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssocItemScopeEntry)) return false;
        AssocItemScopeEntry that = (AssocItemScopeEntry) o;
        return name.equals(that.name) && element.equals(that.element)
            && namespaces.equals(that.namespaces) && subst.equals(that.subst)
            && selfTy.equals(that.selfTy) && source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, element, namespaces, subst, selfTy, source);
    }
}
