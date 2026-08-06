/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFieldLookup;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFieldDecl;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.RsInferenceResult;

import java.util.Collections;
import java.util.List;

public class RsFieldLookupReferenceImpl extends RsReferenceBase<RsFieldLookup> {

    public RsFieldLookupReferenceImpl(@Nonnull RsFieldLookup element) {
        super(element);
    }

    @Nonnull
    @Override
    public List<RsElement> multiResolve() {
        RsInferenceResult inference = ExtensionsUtil.getInference(getElement());
        if (inference != null) {
            List<RsElement> resolved = inference.getResolvedField(getElement());
            if (resolved != null) return resolved;
        }
        return Collections.emptyList();
    }

    @Override
    public PsiElement handleElementRename(@Nonnull String newName) {
        PsiElement ident = getElement().getIdentifier();
        if (ident != null) doRename(ident, newName);
        return getElement();
    }

    @Override
    public boolean isReferenceTo(@Nonnull PsiElement element) {
        return element instanceof RsFieldDecl && super.isReferenceTo(element);
    }
}
