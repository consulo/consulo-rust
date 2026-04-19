/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.*;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;
import org.rust.lang.core.psi.ext.RsTypeParameterUtil;
import org.rust.lang.core.psi.ext.RsPolyboundUtil;

public class LazyParamEnv implements ParamEnv {
    @NotNull
    private final RsGenericDeclaration parentItem;

    public LazyParamEnv(@NotNull RsGenericDeclaration parentItem) {
        this.parentItem = parentItem;
    }

    @NotNull
    @Override
    public Sequence<BoundElement<RsTraitItem>> boundsFor(@NotNull Ty ty) {
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
            } else if (parameter == TyTypeParameter.Self.INSTANCE) {
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

    @NotNull
    private static List<BoundElement<RsTraitItem>> getTraitBoundsFromParameter(@NotNull TyTypeParameter tyParam) {
        TyTypeParameter.TypeParameter parameter = tyParam.getParameter();
        if (parameter instanceof TyTypeParameter.Named) {
            RsTypeParameter rsParam = ((TyTypeParameter.Named) parameter).getParameter();
            List<RsPolybound> bounds = RsTypeParameterUtil.getBounds(rsParam);
            List<BoundElement<RsTraitItem>> result = new ArrayList<>();
            for (RsPolybound polybound : bounds) {
                if (RsPolyboundUtil.getHasQ(polybound)) continue;
                RsBound bound = polybound.getBound();
                if (bound != null) {
                    RsTraitRef traitRef = bound.getTraitRef();
                    if (traitRef != null) {
                        BoundElement<RsTraitItem> resolved = RsTraitRefUtil.resolveToBoundTrait(traitRef);
                        if (resolved != null) {
                            result.add(resolved);
                        }
                    }
                }
            }
            return result;
        }
        return Collections.emptyList();
    }
}
