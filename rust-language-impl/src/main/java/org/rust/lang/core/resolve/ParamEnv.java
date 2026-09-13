/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.infer.RsInferenceContext;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.types.infer.TyWithObligations;
import org.rust.lang.core.psi.ext.impl.*;

/**
 * When type checking, we use the ParamEnv to track details about the set of where-clauses
 * that are in scope at this particular point.
 * Note: ParamEnv of an associated item (method) also contains bounds of its trait/impl
 * Note: callerBounds should have type List&lt;Predicate&gt; to also support lifetime bounds
 */
public interface ParamEnv {

    @Nonnull
    ParamEnv EMPTY = new SimpleParamEnv(Collections.emptyList());

    @Nonnull
    Sequence<BoundElement<RsTraitItem>> boundsFor(@Nonnull Ty ty);

    @Nonnull
    static ParamEnv buildFor(@Nonnull RsItemElement decl) {
        List<TraitRef> rawBounds = new ArrayList<>();

        if (decl instanceof RsGenericDeclaration) {
            RsGenericDeclaration genDecl = (RsGenericDeclaration) decl;
            rawBounds.addAll(RsGenericDeclarationUtil.getBounds(genDecl));
            if (decl instanceof RsTraitItem) {
                rawBounds.add(new TraitRef(TyTypeParameter.self(), RsGenericDeclarationUtil.withDefaultSubst((RsTraitItem) decl)));
            }
        }
        if (decl instanceof RsAbstractable) {
            RsAbstractableOwner owner = ((RsAbstractable) decl).getOwner();
            if (owner instanceof RsAbstractableOwner.Trait) {
                RsTraitItem trait = ((RsAbstractableOwner.Trait) owner).getTrait();
                rawBounds.add(new TraitRef(TyTypeParameter.self(), RsGenericDeclarationUtil.withDefaultSubst(trait)));
                rawBounds.addAll(RsGenericDeclarationUtil.getBounds(trait));
            } else if (owner instanceof RsAbstractableOwner.Impl) {
                RsImplItem impl = ((RsAbstractableOwner.Impl) owner).getImpl();
                rawBounds.addAll(RsGenericDeclarationUtil.getBounds(impl));
            }
        }

        List<TraitRef> flatBounds = new ArrayList<>();
        for (TraitRef bound : rawBounds) {
            flatBounds.addAll(bound.getFlattenHierarchy());
        }
        List<TraitRef> distinctBounds = flatBounds.stream().distinct().collect(Collectors.toList());

        if (distinctBounds.isEmpty()) return EMPTY;
        if (distinctBounds.size() == 1) return new SimpleParamEnv(distinctBounds);

        ImplLookup lookup = new ImplLookup(
            decl.getProject(),
            RsElementUtil.getContainingCrate(decl),
            RsElementUtil.getKnownItems(decl),
            new SimpleParamEnv(distinctBounds)
        );
        RsInferenceContext ctx = lookup.getCtx();
        List<TraitRef> normalizedBounds = new ArrayList<>(distinctBounds.size());
        for (TraitRef ref : distinctBounds) {
            TyWithObligations<TraitRef> normalized = ctx.normalizeAssociatedTypesIn(ref);
            ctx.fulfill.registerPredicateObligations(normalized.getObligations());
            normalizedBounds.add(normalized.getValue());
        }
        ctx.fulfill.selectWherePossible();

        List<TraitRef> resolvedBounds = new ArrayList<>(normalizedBounds.size());
        for (TraitRef ref : normalizedBounds) {
            resolvedBounds.add(ctx.fullyResolve(ref));
        }
        return new SimpleParamEnv(resolvedBounds);
    }

    /**
     * A marker interface for Sequence in this context.
     */
    interface Sequence<T> extends Iterable<T> {
    }
}
