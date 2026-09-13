/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsExternCrateItem;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsExternCrateReferenceImpl extends RsReferenceCached<RsExternCrateItem> {

    public RsExternCrateReferenceImpl(@Nonnull RsExternCrateItem externCrate) {
        super(externCrate);
    }

    @Nonnull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.collectResolveVariants(getElement().getReferenceName(), processor ->
            NameResolution.processExternCrateResolveVariants(getElement(), false, processor)
        );
    }

    @Override
    public boolean isReferenceTo(@Nonnull PsiElement element) {
        return element instanceof RsFile && super.isReferenceTo(element);
    }
}
