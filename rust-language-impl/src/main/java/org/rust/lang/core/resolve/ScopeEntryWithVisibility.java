/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;

import java.util.Objects;
import java.util.Set;

public final class ScopeEntryWithVisibility implements ScopeEntry {
    @Nonnull
    private final String name;
    @Nonnull
    private final RsElement element;
    @Nonnull
    private final Set<Namespace> namespaces;
    @Nonnull
    private final VisibilityFilter visibilityFilter;
    @Nonnull
    private final Substitution subst;

    public ScopeEntryWithVisibility(@Nonnull String name, @Nonnull RsElement element,
                                     @Nonnull Set<Namespace> namespaces,
                                     @Nonnull VisibilityFilter visibilityFilter) {
        this(name, element, namespaces, visibilityFilter, SubstitutionUtil.EMPTY);
    }

    public ScopeEntryWithVisibility(@Nonnull String name, @Nonnull RsElement element,
                                     @Nonnull Set<Namespace> namespaces,
                                     @Nonnull VisibilityFilter visibilityFilter,
                                     @Nonnull Substitution subst) {
        this.name = name;
        this.element = element;
        this.namespaces = namespaces;
        this.visibilityFilter = visibilityFilter;
        this.subst = subst;
    }

    @Nonnull
    @Override
    public String getName() {
        return name;
    }

    @Nonnull
    @Override
    public RsElement getElement() {
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
    public VisibilityFilter getVisibilityFilter() {
        return visibilityFilter;
    }

    @Nonnull
    @Override
    public ScopeEntry copyWithNs(@Nonnull Set<Namespace> namespaces) {
        return new ScopeEntryWithVisibility(name, element, namespaces, visibilityFilter, subst);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScopeEntryWithVisibility)) return false;
        ScopeEntryWithVisibility that = (ScopeEntryWithVisibility) o;
        return name.equals(that.name) && element.equals(that.element)
            && namespaces.equals(that.namespaces) && subst.equals(that.subst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, element, namespaces, subst);
    }
}
