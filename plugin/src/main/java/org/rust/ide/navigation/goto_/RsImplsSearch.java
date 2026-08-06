/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import consulo.project.util.query.QueryExecutorBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.search.DefinitionsScopedSearch;
import java.util.function.Predicate;
import consulo.application.util.query.Query;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsAbstractableUtil;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElementUtil;
import org.rust.openapiext.QueryExtUtil;
import org.rust.lang.core.psi.ext.RsTraitItemUtil;

public class RsImplsSearch extends QueryExecutorBase<PsiElement, DefinitionsScopedSearch.SearchParameters> {

    public RsImplsSearch() {
        super(/* readAction = */ true);
    }

    @Override
    public void processQuery(@Nonnull DefinitionsScopedSearch.SearchParameters queryParameters,
                             @Nonnull Predicate<? super PsiElement> consumer) {
        PsiElement psi = queryParameters.getElement();
        Query<? extends PsiElement> query;

        if (psi instanceof RsStructItem) {
            query = RsStructOrEnumItemElementUtil.searchForImplementations((RsStructItem) psi);
        } else if (psi instanceof RsEnumItem) {
            query = RsStructOrEnumItemElementUtil.searchForImplementations((RsEnumItem) psi);
        } else if (psi instanceof RsTraitItem) {
            query = RsTraitItemUtil.searchForImplementations((RsTraitItem) psi);
        } else if (psi instanceof RsAbstractable) {
            java.util.List<RsAbstractable> impls = RsAbstractableUtil.searchForImplementations((RsAbstractable) psi);
            query = new consulo.application.util.query.CollectionQuery<>(new java.util.ArrayList<>(impls));
        } else {
            return;
        }

        Query<? extends PsiElement> filteredQuery = QueryExtUtil.filterQuery(query, it -> it != null);
        filteredQuery.forEach((consulo.application.util.function.Processor<PsiElement>) consumer::test);
    }
}
