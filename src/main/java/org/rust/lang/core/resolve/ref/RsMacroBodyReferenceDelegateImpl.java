/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.MacroExpansionUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsReferenceElementBase;
import org.rust.openapiext.Testmark;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public class RsMacroBodyReferenceDelegateImpl extends RsReferenceBase<RsReferenceElementBase> {

    public RsMacroBodyReferenceDelegateImpl(@NotNull RsReferenceElementBase element) {
        super(element);
    }

    @NotNull
    public List<RsReference> getDelegates() {
        Testmarks.Touched.hit();
        List<PsiElement> expansionElements = MacroExpansionUtil.findExpansionElements(getElement());
        if (expansionElements == null) return Collections.emptyList();
        List<RsReference> result = new ArrayList<>();
        for (PsiElement delegated : expansionElements) {
            PsiElement current = delegated;
            while (current != null) {
                if (current.getReference() instanceof RsReference) {
                    result.add((RsReference) current.getReference());
                    break;
                }
                current = current.getParent();
            }
        }
        return result;
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        for (RsReference delegate : getDelegates()) {
            if (delegate.isReferenceTo(element)) return true;
        }
        return false;
    }

    @NotNull
    @Override
    public List<RsElement> multiResolve() {
        LinkedHashSet<RsElement> result = new LinkedHashSet<>();
        for (RsReference delegate : getDelegates()) {
            result.addAll(delegate.multiResolve());
        }
        return new ArrayList<>(result);
    }

    public static class Testmarks {
        public static final Testmark Touched = new Testmark();
    }
}
