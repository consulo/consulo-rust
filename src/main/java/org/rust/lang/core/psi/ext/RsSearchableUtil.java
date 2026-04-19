/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;

import java.util.Collection;
import java.util.Collections;

public final class RsSearchableUtil {
    private RsSearchableUtil() {}

    @NotNull
    public static Collection<PsiReference> searchReferences(@NotNull RsNamedElement element, @NotNull SearchScope scope) {
        return com.intellij.psi.search.searches.ReferencesSearch.search(element, scope).findAll();
    }
}
