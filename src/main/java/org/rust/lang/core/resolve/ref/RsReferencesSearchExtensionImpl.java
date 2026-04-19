/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsTupleFieldDecl;
import org.rust.lang.core.psi.ext.RsFieldsOwner;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

public class RsReferencesSearchExtensionImpl extends QueryExecutorBase<RsReference, ReferencesSearch.SearchParameters> {

    public RsReferencesSearchExtensionImpl() {
        super(true);
    }

    @Override
    public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super RsReference> consumer) {
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
