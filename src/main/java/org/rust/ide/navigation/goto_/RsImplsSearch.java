/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
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
    public void processQuery(@NotNull DefinitionsScopedSearch.SearchParameters queryParameters,
                             @NotNull Processor<? super PsiElement> consumer) {
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
            query = new com.intellij.util.CollectionQuery<>(new java.util.ArrayList<>(impls));
        } else {
            return;
        }

        Query<? extends PsiElement> filteredQuery = QueryExtUtil.filterQuery(query, it -> it != null);
        filteredQuery.forEach((Processor<PsiElement>) consumer::process);
    }
}
