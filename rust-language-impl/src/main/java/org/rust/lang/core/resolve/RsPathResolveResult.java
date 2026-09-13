/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import consulo.language.psi.PsiElement;
import consulo.language.psi.ResolveResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.Substitution;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Used as a resolve result in {@link org.rust.lang.core.resolve.ref.RsPathReferenceImpl}
 */
public class RsPathResolveResult<T extends RsElement> implements ResolveResult {
    @Nonnull
    private final T myElement;
    @Nonnull
    private final Substitution myResolvedSubst;
    private final boolean myIsVisible;
    @Nonnull
    private final Set<Namespace> myNamespaces;

    public RsPathResolveResult(@Nonnull T element, @Nonnull Substitution resolvedSubst, boolean isVisible, @Nonnull Set<Namespace> namespaces) {
        myElement = element;
        myResolvedSubst = resolvedSubst;
        myIsVisible = isVisible;
        myNamespaces = namespaces;
    }

    public RsPathResolveResult(@Nonnull T element, @Nonnull Substitution resolvedSubst, boolean isVisible) {
        this(element, resolvedSubst, isVisible, Collections.emptySet());
    }

    public RsPathResolveResult(@Nonnull T element, boolean isVisible) {
        this(element, Substitution.getEMPTY(), isVisible, Collections.emptySet());
    }

    @Nonnull
    public T element() {
        return myElement;
    }

    @Nonnull
    public Substitution getResolvedSubst() {
        return myResolvedSubst;
    }

    public boolean isVisible() {
        return myIsVisible;
    }

    @Nonnull
    public Set<Namespace> getNamespaces() {
        return myNamespaces;
    }

    @Nullable
    @Override
    public PsiElement getElement() {
        return myElement;
    }

    @Override
    public boolean isValidResult() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RsPathResolveResult<?> that = (RsPathResolveResult<?>) o;
        return myIsVisible == that.myIsVisible &&
            Objects.equals(myElement, that.myElement) &&
            Objects.equals(myResolvedSubst, that.myResolvedSubst) &&
            Objects.equals(myNamespaces, that.myNamespaces);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myElement, myResolvedSubst, myIsVisible, myNamespaces);
    }

    @Override
    public String toString() {
        return "RsPathResolveResult(" +
            "element=" + myElement +
            ", resolvedSubst=" + myResolvedSubst +
            ", isVisible=" + myIsVisible +
            ", namespaces=" + myNamespaces +
            ')';
    }
}
