/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsLabel;
import org.rust.lang.core.psi.RsLabelDecl;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsLabelReferenceImpl extends RsReferenceCached<RsLabel> {

    public RsLabelReferenceImpl(@NotNull RsLabel element) {
        super(element);
    }

    @NotNull
    @Override
    protected ResolveCacheDependency getCacheDependency() {
        return ResolveCacheDependency.LOCAL;
    }

    @NotNull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.resolveLabelReference(getElement(), false);
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return element instanceof RsLabelDecl && super.isReferenceTo(element);
    }
}
