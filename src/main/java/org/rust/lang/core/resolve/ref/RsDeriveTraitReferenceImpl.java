/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsTraitItemImplUtil;
import org.rust.lang.core.resolve.NameResolution;

import java.util.ArrayList;
import java.util.List;

public class RsDeriveTraitReferenceImpl extends RsReferenceCached<RsPath> implements RsPathReference {

    public RsDeriveTraitReferenceImpl(@NotNull RsPath element) {
        super(element);
    }

    @NotNull
    @Override
    protected List<RsElement> resolveInner() {
        List<RsElement> derivedTraits = resolveToDerivedTrait(getElement());
        List<RsElement> knownDerivable = new ArrayList<>();
        for (RsElement trait : derivedTraits) {
            if (trait instanceof RsTraitItem && RsTraitItemImplUtil.isKnownDerivable((RsTraitItem) trait)) {
                knownDerivable.add(trait);
            }
        }
        if (!knownDerivable.isEmpty()) {
            return knownDerivable;
        }
        return resolveToProcMacro(getElement());
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement element) {
        return (element instanceof RsTraitItem || element instanceof RsFunction) && super.isReferenceTo(element);
    }

    @NotNull
    private static List<RsElement> resolveToDerivedTrait(@NotNull RsPath path) {
        String traitName = path.getReferenceName();
        if (traitName == null) return java.util.Collections.emptyList();
        List<RsElement> variants = NameResolution.collectResolveVariants(traitName, processor ->
            NameResolution.processDeriveTraitResolveVariants(path, traitName, processor)
        );
        List<RsElement> result = new ArrayList<>();
        for (RsElement v : variants) {
            if (v instanceof RsTraitItem) {
                result.add(v);
            }
        }
        return result;
    }

    @NotNull
    private static List<RsElement> resolveToProcMacro(@NotNull RsPath path) {
        String traitName = path.getReferenceName();
        if (traitName == null) return java.util.Collections.emptyList();
        return NameResolution.collectResolveVariants(traitName, processor ->
            NameResolution.processProcMacroResolveVariants(path, processor, false)
        );
    }
}
