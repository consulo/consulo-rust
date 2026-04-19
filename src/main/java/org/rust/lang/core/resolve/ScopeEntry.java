/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
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
    @NotNull
    String getName();

    @NotNull
    RsElement getElement();

    @NotNull
    Set<Namespace> getNamespaces();

    @NotNull
    default Substitution getSubst() {
        return SubstitutionUtil.EMPTY;
    }

    @NotNull
    ScopeEntry copyWithNs(@NotNull Set<Namespace> namespaces);
}
