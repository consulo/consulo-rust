/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsAttributeProcMacroReferenceImpl extends RsReferenceCached<RsPath> implements RsPathReference {

    public RsAttributeProcMacroReferenceImpl(@Nonnull RsPath element) {
        super(element);
    }

    @Nonnull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.collectResolveVariants(getElement().getReferenceName(), processor ->
            NameResolution.processProcMacroResolveVariants(getElement(), processor, false)
        );
    }

    @Override
    public boolean isReferenceTo(@Nonnull PsiElement element) {
        return element instanceof RsFunction && super.isReferenceTo(element);
    }
}
