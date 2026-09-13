/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsTraitItemImplUtil;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.resolve.Namespace;

import java.util.ArrayList;
import java.util.List;

public class RsDeriveTraitReferenceImpl extends RsReferenceCached<RsPath> implements RsPathReference {

    public RsDeriveTraitReferenceImpl(@Nonnull RsPath element) {
        super(element);
    }

    @Nonnull
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
        RsTraitItem inScope = findTraitInScope(getElement());
        if (inScope != null) {
            return List.of(inScope);
        }
        return resolveToProcMacro(getElement());
    }

    /**
     * The trait a custom derive provides, when one of that name is in scope where the derive is
     * written - {@code clap::Parser} for {@code #[derive(Parser)]} under {@code use clap::Parser}.
     * <p>
     * A derive macro and the trait it implements share a name by convention, and the macro itself
     * cannot be expanded here, so the trait is the only way to see the members it would generate.
     * The lookup goes through the scope rather than the by-name index on purpose: several crates
     * declare a trait called {@code Parser}, and only the imported one is the right answer.
     */
    @Nullable
    private static RsTraitItem findTraitInScope(@Nonnull RsPath path) {
        String traitName = path.getReferenceName();
        if (traitName == null) return null;
        RsNamedElement resolved = NameResolution.findInScope(path, traitName, Namespace.TYPES);
        return resolved instanceof RsTraitItem ? (RsTraitItem) resolved : null;
    }

    @Override
    public boolean isReferenceTo(@Nonnull PsiElement element) {
        return (element instanceof RsTraitItem || element instanceof RsFunction) && super.isReferenceTo(element);
    }

    @Nonnull
    private static List<RsElement> resolveToDerivedTrait(@Nonnull RsPath path) {
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

    @Nonnull
    private static List<RsElement> resolveToProcMacro(@Nonnull RsPath path) {
        String traitName = path.getReferenceName();
        if (traitName == null) return java.util.Collections.emptyList();
        return NameResolution.collectResolveVariants(traitName, processor ->
            NameResolution.processProcMacroResolveVariants(path, processor, false)
        );
    }
}
