/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.*;
import org.rust.lang.core.psi.ext.impl.RsTraitRefUtil;
import org.rust.lang.core.psi.ext.impl.RsTypeParameterUtil;
import org.rust.lang.core.psi.ext.impl.RsPolyboundUtil;
import org.rust.lang.core.psi.ext.impl.*;

public class LazyParamEnv implements ParamEnv {
    @Nonnull
    private final RsGenericDeclaration parentItem;

    public LazyParamEnv(@Nonnull RsGenericDeclaration parentItem) {
        this.parentItem = parentItem;
    }

    @Nonnull
    @Override
    public Sequence<BoundElement<RsTraitItem>> boundsFor(@Nonnull Ty ty) {
        if (ty instanceof TyTypeParameter) {
            TyTypeParameter tyParam = (TyTypeParameter) ty;
            List<BoundElement<RsTraitItem>> additionalBounds = new ArrayList<>();

            TyTypeParameter.TypeParameter parameter = tyParam.getParameter();
            if (parameter instanceof TyTypeParameter.Named) {
                TyTypeParameter.Named named = (TyTypeParameter.Named) parameter;
                if (RsTypeParameterUtil.getOwner(named.getParameter()) != parentItem) {
                    RsWhereClause whereClause = parentItem.getWhereClause();
                    if (whereClause != null) {
                        for (RsWherePred pred : whereClause.getWherePredList()) {
                            RsTypeReference typeRef = pred.getTypeReference();
                            if (typeRef != null) {
                                RsTypeReference skipped = RsTypeReferenceUtil.skipParens(typeRef);
                                if (skipped instanceof RsPathType) {
                                    RsPath path = ((RsPathType) skipped).getPath();
                                    if (path != null && path.getReference() != null
                                        && path.getReference().resolve() == named.getParameter()) {
                                        RsTypeParamBounds bounds = pred.getTypeParamBounds();
                                        if (bounds != null) {
                                            for (RsPolybound polybound : bounds.getPolyboundList()) {
                                                if (RsPolyboundUtil.getHasQ(polybound)) continue;
                                                RsBound bound = polybound.getBound();
                                                if (bound != null) {
                                                    RsTraitRef traitRef = bound.getTraitRef();
                                                    if (traitRef != null) {
                                                        BoundElement<RsTraitItem> resolved = RsTraitRefUtil.resolveToBoundTrait(traitRef);
                                                        if (resolved != null) {
                                                            additionalBounds.add(resolved);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (parameter instanceof TyTypeParameter.Self) {
                if (!(parentItem instanceof RsTraitOrImpl)) {
                    RsWhereClause whereClause = parentItem.getWhereClause();
                    if (whereClause != null) {
                        for (RsWherePred pred : whereClause.getWherePredList()) {
                            RsTypeReference typeRef = pred.getTypeReference();
                            if (typeRef != null) {
                                RsTypeReference skipped = RsTypeReferenceUtil.skipParens(typeRef);
                                if (skipped instanceof RsPathType) {
                                    RsPath path = ((RsPathType) skipped).getPath();
                                    if (path != null && path.getKind() == PathKind.CSELF) {
                                        RsTypeParamBounds bounds = pred.getTypeParamBounds();
                                        if (bounds != null) {
                                            for (RsPolybound polybound : bounds.getPolyboundList()) {
                                                if (RsPolyboundUtil.getHasQ(polybound)) continue;
                                                RsBound bound = polybound.getBound();
                                                if (bound != null) {
                                                    RsTraitRef traitRef = bound.getTraitRef();
                                                    if (traitRef != null) {
                                                        BoundElement<RsTraitItem> resolved = RsTraitRefUtil.resolveToBoundTrait(traitRef);
                                                        if (resolved != null) {
                                                            additionalBounds.add(resolved);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            List<BoundElement<RsTraitItem>> result = new ArrayList<>(getTraitBoundsFromParameter(tyParam));
            result.addAll(additionalBounds);
            return result::iterator;
        } else {
            return Collections.<BoundElement<RsTraitItem>>emptyList()::iterator;
        }
    }

    /** The bounds the parameter itself declares, with their supertraits. */
    @Nonnull
    private static List<BoundElement<RsTraitItem>> getTraitBoundsFromParameter(@Nonnull TyTypeParameter tyParam) {
        List<BoundElement<RsTraitItem>> direct = new ArrayList<>();
        TyTypeParameter.TypeParameter parameter = tyParam.getParameter();
        if (parameter instanceof TyTypeParameter.Named) {
            RsTypeParameter rsParam = ((TyTypeParameter.Named) parameter).getParameter();
            for (RsPolybound polybound : RsTypeParameterUtil.getBounds(rsParam)) {
                if (RsPolyboundUtil.getHasQ(polybound)) continue;
                RsBound bound = polybound.getBound();
                RsTraitRef traitRef = bound != null ? bound.getTraitRef() : null;
                BoundElement<RsTraitItem> resolved = traitRef != null ? RsTraitRefUtil.resolveToBoundTrait(traitRef) : null;
                if (resolved != null) direct.add(resolved);
            }
        }
        else if (parameter instanceof TyTypeParameter.Self self && self.getTraitOrImpl() != null) {
            BoundElement<RsTraitItem> implemented = RsTraitOrImplUtil.getImplementedTrait(self.getTraitOrImpl());
            if (implemented != null) direct.add(implemented);
        }
        List<BoundElement<RsTraitItem>> result = new ArrayList<>();
        for (BoundElement<RsTraitItem> bound : direct) {
            result.addAll(RsTraitItemUtil.getFlattenHierarchy(bound));
        }
        return result;
    }
}
