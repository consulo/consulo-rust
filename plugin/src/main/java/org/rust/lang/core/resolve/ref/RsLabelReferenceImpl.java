/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsLabel;
import org.rust.lang.core.psi.RsLabelDecl;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsLabelReferenceImpl extends RsReferenceCached<RsLabel> {

    public RsLabelReferenceImpl(@Nonnull RsLabel element) {
        super(element);
    }

    @Nonnull
    @Override
    protected ResolveCacheDependency getCacheDependency() {
        return ResolveCacheDependency.LOCAL;
    }

    @Nonnull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.resolveLabelReference(getElement(), false);
    }

    @Override
    public boolean isReferenceTo(@Nonnull PsiElement element) {
        return element instanceof RsLabelDecl && super.isReferenceTo(element);
    }
}
