/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMacroBinding;
import org.rust.lang.core.psi.RsMacroReference;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsMacroReferenceImpl extends RsReferenceCached<RsMacroReference> {

    public RsMacroReferenceImpl(@Nonnull RsMacroReference pattern) {
        super(pattern);
    }

    @Nonnull
    @Override
    protected ResolveCacheDependency getCacheDependency() {
        return ResolveCacheDependency.LOCAL;
    }

    @Nonnull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.collectResolveVariants(getElement().getReferenceName(), processor ->
            NameResolution.processMacroReferenceVariants(getElement(), processor)
        );
    }

    @Override
    public boolean isReferenceTo(@Nonnull PsiElement element) {
        return element instanceof RsMacroBinding && super.isReferenceTo(element);
    }
}
