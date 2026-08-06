/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiReference;
import consulo.content.scope.SearchScope;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;

import java.util.Collection;
import java.util.Collections;

public final class RsSearchableUtil {
    private RsSearchableUtil() {}

    @Nonnull
    public static Collection<PsiReference> searchReferences(@Nonnull RsNamedElement element, @Nonnull SearchScope scope) {
        return consulo.language.psi.search.ReferencesSearch.search(element, scope).findAll();
    }
}
