/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ty.Ty;

import java.util.Objects;
import java.util.Set;

public final class AssocItemScopeEntry implements ScopeEntry {
    @Nonnull
    private final String name;
    @Nonnull
    private final RsAbstractable element;
    @Nonnull
    private final Set<Namespace> namespaces;
    @Nonnull
    private final Substitution subst;
    @Nonnull
    private final Ty selfTy;
    @Nonnull
    private final TraitImplSource source;

    public AssocItemScopeEntry(@Nonnull String name, @Nonnull RsAbstractable element,
                                @Nonnull Set<Namespace> namespaces, @Nonnull Substitution subst,
                                @Nonnull Ty selfTy, @Nonnull TraitImplSource source) {
        this.name = name;
        this.element = element;
        this.namespaces = namespaces;
        this.subst = subst;
        this.selfTy = selfTy;
        this.source = source;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Nonnull
    @Override
    public RsAbstractable getElement() {
        return element;
    }

    @Nonnull
    @Override
    public Set<Namespace> getNamespaces() {
        return namespaces;
    }

    @Nonnull
    @Override
    public Substitution getSubst() {
        return subst;
    }

    @Nonnull
    public Ty getSelfTy() {
        return selfTy;
    }

    @Nonnull
    public TraitImplSource getSource() {
        return source;
    }

    @Nonnull
    @Override
    public ScopeEntry copyWithNs(@Nonnull Set<Namespace> namespaces) {
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
