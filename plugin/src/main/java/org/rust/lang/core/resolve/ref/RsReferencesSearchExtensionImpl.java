/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.project.util.query.QueryExecutorBase;
import consulo.language.psi.search.UsageSearchContext;
import consulo.language.psi.search.ReferencesSearch;
import java.util.function.Predicate;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsTupleFieldDecl;
import org.rust.lang.core.psi.ext.RsFieldsOwner;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

public class RsReferencesSearchExtensionImpl extends QueryExecutorBase<RsReference, ReferencesSearch.SearchParameters> {

    public RsReferencesSearchExtensionImpl() {
        super(true);
    }

    @Override
    public void processQuery(@Nonnull ReferencesSearch.SearchParameters queryParameters, @Nonnull Predicate<? super RsReference> consumer) {
        Object element = queryParameters.getElementToSearch();
        if (element instanceof RsTupleFieldDecl) {
            RsTupleFieldDecl tupleField = (RsTupleFieldDecl) element;
            RsFieldsOwner elementOwnerStruct = RsPsiJavaUtil.ancestorStrict(tupleField, RsFieldsOwner.class);
            if (elementOwnerStruct == null) return;
            if (elementOwnerStruct.getTupleFields() == null) return;
            int elementIndex = elementOwnerStruct.getTupleFields().getTupleFieldDeclList().indexOf(tupleField);
            queryParameters.getOptimizer().searchWord(
                String.valueOf(elementIndex),
                queryParameters.getEffectiveSearchScope(),
                UsageSearchContext.IN_CODE,
                false,
                tupleField
            );
        } else if (element instanceof RsFile) {
            RsFile rsFile = (RsFile) element;
            if (rsFile.getOwnedDirectory() != null) {
                queryParameters.getOptimizer().searchWord(
                    rsFile.getOwnedDirectory().getName(),
                    queryParameters.getEffectiveSearchScope(),
                    true,
                    rsFile
                );
            }
        }
    }
}
