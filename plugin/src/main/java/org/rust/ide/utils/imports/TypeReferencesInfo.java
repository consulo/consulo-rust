/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.imports;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

import java.util.Set;

/**
 *
 * @param toImport  Set of unresolved items that should be imported
 * @param toQualify Set of unresolved items that can't be imported
 */
public final class TypeReferencesInfo {
    @Nonnull
    private final Set<RsQualifiedNamedElement> toImport;
    @Nonnull
    private final Set<RsQualifiedNamedElement> toQualify;

    public TypeReferencesInfo(
        @Nonnull Set<RsQualifiedNamedElement> toImport,
        @Nonnull Set<RsQualifiedNamedElement> toQualify
    ) {
        this.toImport = toImport;
        this.toQualify = toQualify;
    }

    @Nonnull
    public Set<RsQualifiedNamedElement> getToImport() {
        return toImport;
    }

    @Nonnull
    public Set<RsQualifiedNamedElement> getToQualify() {
        return toQualify;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeReferencesInfo that = (TypeReferencesInfo) o;
        return toImport.equals(that.toImport) && toQualify.equals(that.toQualify);
    }

    @Override
    public int hashCode() {
        int result = toImport.hashCode();
        result = 31 * result + toQualify.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TypeReferencesInfo(toImport=" + toImport + ", toQualify=" + toQualify + ")";
    }
}
