/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.Substitution;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Used as a resolve result in {@link org.rust.lang.core.resolve.ref.RsPathReferenceImpl}
 */
public class RsPathResolveResult<T extends RsElement> implements ResolveResult {
    @NotNull
    private final T myElement;
    @NotNull
    private final Substitution myResolvedSubst;
    private final boolean myIsVisible;
    @NotNull
    private final Set<Namespace> myNamespaces;

    public RsPathResolveResult(@NotNull T element, @NotNull Substitution resolvedSubst, boolean isVisible, @NotNull Set<Namespace> namespaces) {
        myElement = element;
        myResolvedSubst = resolvedSubst;
        myIsVisible = isVisible;
        myNamespaces = namespaces;
    }

    public RsPathResolveResult(@NotNull T element, @NotNull Substitution resolvedSubst, boolean isVisible) {
        this(element, resolvedSubst, isVisible, Collections.emptySet());
    }

    public RsPathResolveResult(@NotNull T element, boolean isVisible) {
        this(element, Substitution.getEMPTY(), isVisible, Collections.emptySet());
    }

    @NotNull
    public T element() {
        return myElement;
    }

    @NotNull
    public Substitution getResolvedSubst() {
        return myResolvedSubst;
    }

    public boolean isVisible() {
        return myIsVisible;
    }

    @NotNull
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
