/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMacroBinding;
import org.rust.lang.core.psi.RsMacroReference;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsMacroReferenceImpl extends RsReferenceCached<RsMacroReference> {

    public RsMacroReferenceImpl(@NotNull RsMacroReference pattern) {
        super(pattern);
    }

    @NotNull
    @Override
    protected ResolveCacheDependency getCacheDependency() {
        return ResolveCacheDependency.LOCAL;
    }

    @NotNull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.collectResolveVariants(getElement().getReferenceName(), processor ->
            NameResolution.processMacroReferenceVariants(getElement(), processor)
        );
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return element instanceof RsMacroBinding && super.isReferenceTo(element);
    }
}
