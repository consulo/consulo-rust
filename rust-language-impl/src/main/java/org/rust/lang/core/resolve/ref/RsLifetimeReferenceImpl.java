/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsLifetime;
import org.rust.lang.core.psi.RsLifetimeParameter;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsLifetimeReferenceImpl extends RsReferenceCached<RsLifetime> {

    public RsLifetimeReferenceImpl(@Nonnull RsLifetime element) {
        super(element);
    }

    @Nonnull
    @Override
    protected ResolveCacheDependency getCacheDependency() {
        return ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE;
    }

    @Nonnull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.collectResolveVariants(getElement().getReferenceName(), processor ->
            NameResolution.processLifetimeResolveVariants(getElement(), processor)
        );
    }

    @Override
    public boolean isReferenceTo(@Nonnull PsiElement element) {
        return (element instanceof RsLifetimeParameter || element instanceof RsLifetime) && super.isReferenceTo(element);
    }
}
