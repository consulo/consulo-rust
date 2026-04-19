/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsLifetime;
import org.rust.lang.core.psi.RsLifetimeParameter;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsLifetimeReferenceImpl extends RsReferenceCached<RsLifetime> {

    public RsLifetimeReferenceImpl(@NotNull RsLifetime element) {
        super(element);
    }

    @NotNull
    @Override
    protected ResolveCacheDependency getCacheDependency() {
        return ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE;
    }

    @NotNull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.collectResolveVariants(getElement().getReferenceName(), processor ->
            NameResolution.processLifetimeResolveVariants(getElement(), processor)
        );
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return (element instanceof RsLifetimeParameter || element instanceof RsLifetime) && super.isReferenceTo(element);
    }
}
