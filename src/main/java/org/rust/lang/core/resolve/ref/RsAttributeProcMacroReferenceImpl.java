/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsAttributeProcMacroReferenceImpl extends RsReferenceCached<RsPath> implements RsPathReference {

    public RsAttributeProcMacroReferenceImpl(@NotNull RsPath element) {
        super(element);
    }

    @NotNull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.collectResolveVariants(getElement().getReferenceName(), processor ->
            NameResolution.processProcMacroResolveVariants(getElement(), processor, false)
        );
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return element instanceof RsFunction && super.isReferenceTo(element);
    }
}
