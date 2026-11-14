/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.light;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.ReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.language.psi.search.UsageSearchContext;
import consulo.project.util.query.QueryExecutorBase;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.Predicate;

/**
 * The usages of a source element include the Rust usages of the code generated from it: finding the usages of a
 * protobuf message finds the Rust code that uses its struct, and renaming the message renames them.
 */
@ExtensionImpl
public final class RsLightOriginReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>
    implements ReferencesSearchQueryExecutor {

    @Override
    public void processQuery(@Nonnull ReferencesSearch.SearchParameters parameters, @Nonnull Predicate<? super PsiReference> consumer) {
        PsiElement origin = parameters.getElementToSearch();
        if (origin instanceof RsLightElement) return;
        List<RsLightElement<?>> lightElements = ReadAction.compute(() -> RsLightElements.getInstance(origin.getProject()).forOrigin(origin));
        for (RsLightElement<?> light : lightElements) {
            String name = ReadAction.compute(() -> light instanceof PsiNamedElement named ? named.getName() : null);
            if (name == null) continue;
            parameters.getOptimizer().searchWord(name, parameters.getEffectiveSearchScope(), UsageSearchContext.IN_CODE, true, light);
        }
    }
}
