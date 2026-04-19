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

public final class SimpleScopeEntry implements ScopeEntry {
    @NotNull
    private final String name;
    @NotNull
    private final RsElement element;
    @NotNull
    private final Set<Namespace> namespaces;
    @NotNull
    private final Substitution subst;

    public SimpleScopeEntry(@NotNull String name, @NotNull RsElement element, @NotNull Set<Namespace> namespaces) {
        this(name, element, namespaces, SubstitutionUtil.EMPTY);
    }

    public SimpleScopeEntry(@NotNull String name, @NotNull RsElement element,
                            @NotNull Set<Namespace> namespaces, @NotNull Substitution subst) {
        this.name = name;
        this.element = element;
        this.namespaces = namespaces;
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
    @Override
    public ScopeEntry copyWithNs(@NotNull Set<Namespace> namespaces) {
        return new SimpleScopeEntry(name, element, namespaces, subst);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleScopeEntry)) return false;
        SimpleScopeEntry that = (SimpleScopeEntry) o;
        return name.equals(that.name) && element.equals(that.element)
            && namespaces.equals(that.namespaces) && subst.equals(that.subst);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, element, namespaces, subst);
    }

    @Override
    public String toString() {
        return "SimpleScopeEntry(name=" + name + ", element=" + element + ", namespaces=" + namespaces + ")";
    }
}
