/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsExternCrateItem;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsExternCrateReferenceImpl extends RsReferenceCached<RsExternCrateItem> {

    public RsExternCrateReferenceImpl(@NotNull RsExternCrateItem externCrate) {
        super(externCrate);
    }

    @NotNull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.collectResolveVariants(getElement().getReferenceName(), processor ->
            NameResolution.processExternCrateResolveVariants(getElement(), false, processor)
        );
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return element instanceof RsFile && super.isReferenceTo(element);
    }
}
