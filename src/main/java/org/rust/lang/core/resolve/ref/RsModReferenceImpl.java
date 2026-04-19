/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsModReferenceImpl extends RsReferenceCached<RsModDeclItem> {

    public RsModReferenceImpl(@NotNull RsModDeclItem modDecl) {
        super(modDecl);
    }

    @NotNull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.collectResolveVariants(getElement().getReferenceName(), processor ->
            NameResolution.processModDeclResolveVariants(getElement(), processor)
        );
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return element instanceof RsFile && super.isReferenceTo(element);
    }
}
