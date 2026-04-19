/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;

import java.util.Objects;
import java.util.Set;

public final class ScopeEntryWithVisibility implements ScopeEntry {
    @NotNull
    private final String name;
    @NotNull
    private final RsElement element;
    @NotNull
    private final Set<Namespace> namespaces;
    @NotNull
    private final VisibilityFilter visibilityFilter;
    @NotNull
    private final Substitution subst;

    public ScopeEntryWithVisibility(@NotNull String name, @NotNull RsElement element,
                                     @NotNull Set<Namespace> namespaces,
                                     @NotNull VisibilityFilter visibilityFilter) {
        this(name, element, namespaces, visibilityFilter, SubstitutionUtil.EMPTY);
    }

    public ScopeEntryWithVisibility(@NotNull String name, @NotNull RsElement element,
                                     @NotNull Set<Namespace> namespaces,
                                     @NotNull VisibilityFilter visibilityFilter,
                                     @NotNull Substitution subst) {
        this.name = name;
        this.element = element;
        this.namespaces = namespaces;
        this.visibilityFilter = visibilityFilter;
        this.subst = subst;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public RsElement getElement() {
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
    public VisibilityFilter getVisibilityFilter() {
        return visibilityFilter;
    }

    @NotNull
    @Override
    public ScopeEntry copyWithNs(@NotNull Set<Namespace> namespaces) {
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
