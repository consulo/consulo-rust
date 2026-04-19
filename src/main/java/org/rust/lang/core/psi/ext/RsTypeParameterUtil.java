/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RsTypeParameterUtil {
    private RsTypeParameterUtil() {
    }

    @Nullable
    public static RsGenericDeclaration getOwner(@NotNull RsTypeParameter param) {
        PsiElement parent = param.getParent();
        if (parent == null) return null;
        PsiElement grandParent = parent.getParent();
        return grandParent instanceof RsGenericDeclaration ? (RsGenericDeclaration) grandParent : null;
    }

    /**
     * Returns all bounds for type parameter.
     * Don't use it for stub creation because it will cause IndexNotReadyException!
     */
    @NotNull
    public static List<RsPolybound> getBounds(@NotNull RsTypeParameter param) {
        RsGenericDeclaration owner = getOwner(param);
        List<RsPolybound> whereBounds = new ArrayList<>();
        if (owner != null) {
            for (RsWherePred pred : RsGenericDeclarationUtil.getWherePreds(owner)) {
                RsTypeReference typeRef = pred.getTypeReference();
                if (typeRef != null) {
                    RsTypeReference skipped = RsTypeReferenceExtUtil.skipParens(typeRef);
                    if (skipped instanceof RsPathType) {
                        RsPathType pathType = (RsPathType) skipped;
                        if (pathType.getPath().getReference() != null
                            && pathType.getPath().getReference().resolve() == param) {
                            RsTypeParamBounds bounds = pred.getTypeParamBounds();
                            if (bounds != null) {
                                whereBounds.addAll(bounds.getPolyboundList());
                            }
                        }
                    }
                }
            }
        }

        List<RsPolybound> directBounds = new ArrayList<>();
        RsTypeParamBounds tpb = param.getTypeParamBounds();
        if (tpb != null) {
            directBounds.addAll(tpb.getPolyboundList());
        }
        directBounds.addAll(whereBounds);
        return directBounds;
    }

    public static boolean isSized(@NotNull RsTypeParameter param) {
        RsGenericDeclaration owner = getOwner(param);
        List<RsPolybound> whereBounds = new ArrayList<>();
        if (owner != null) {
            for (RsWherePred pred : RsGenericDeclarationUtil.getWherePreds(owner)) {
                RsTypeReference typeRef = pred.getTypeReference();
                if (typeRef != null) {
                    RsTypeReference skipped = RsTypeReferenceExtUtil.skipParens(typeRef);
                    if (skipped instanceof RsPathType) {
                        RsPathType pathType = (RsPathType) skipped;
                        String refName = pathType.getPath().getReferenceName();
                        if (refName != null && refName.equals(param.getName())) {
                            RsTypeParamBounds bounds = pred.getTypeParamBounds();
                            if (bounds != null) {
                                whereBounds.addAll(bounds.getPolyboundList());
                            }
                        }
                    }
                }
            }
        }

        List<RsPolybound> bounds = new ArrayList<>();
        RsTypeParamBounds tpb = param.getTypeParamBounds();
        if (tpb != null) {
            bounds.addAll(tpb.getPolyboundList());
        }
        bounds.addAll(whereBounds);

        for (RsPolybound bound : bounds) {
            if (RsPolyboundUtil.getHasQ(bound)) {
                return false;
            }
        }
        return true;
    }
}
