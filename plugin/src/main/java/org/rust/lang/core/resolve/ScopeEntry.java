/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;

import java.util.Set;

/**
 * ScopeEntry is some PsiElement visible in some code scope.
 * <p>
 * ScopeEntry handles two cases:
 * <ul>
 *   <li>aliases (that's why we need a name property)</li>
 *   <li>lazy resolving of actual elements (that's why element can return null)</li>
 * </ul>
 */
public interface ScopeEntry {
    @Nonnull
    String getName();

    @Nonnull
    RsElement getElement();

    @Nonnull
    Set<Namespace> getNamespaces();

    @Nonnull
    default Substitution getSubst() {
        return SubstitutionUtil.EMPTY;
    }

    @Nonnull
    ScopeEntry copyWithNs(@Nonnull Set<Namespace> namespaces);
}
