/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.rust.lang.core.resolve.ref.RsPathReference;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;

public final class RsPatUtil {
    private RsPatUtil() {
    }

    public static boolean isIrrefutable(@Nonnull RsPat pat) {
        pat = skipUnnecessaryTupDown(pat);
        if (pat instanceof RsPatSlice) {
            List<RsPat> nested = ((RsPatSlice) pat).getPatList();
            if (nested.size() == 1) {
                RsPat single = nested.get(0);
                if (single instanceof RsPatRest) return true;
                if (single instanceof RsPatIdent) return ((RsPatIdent) single).getPat() instanceof RsPatRest;
            }
            return false;
        }
        if (pat instanceof RsPatTup) {
            for (RsPat p : ((RsPatTup) pat).getPatList()) {
                if (!isIrrefutable(p)) return false;
            }
            return true;
        }
        if (pat instanceof RsPatBox) {
            return isIrrefutable(((RsPatBox) pat).getPat());
        }
        if (pat instanceof RsPatRef) {
            return isIrrefutable(((RsPatRef) pat).getPat());
        }
        if (pat instanceof RsPatStruct) {
            RsPatStruct patStruct = (RsPatStruct) pat;
            if (!isIrrefutablePath(patStruct.getPath())) return false;
            for (RsPatField field : patStruct.getPatFieldList()) {
                RsPatFieldFull full = field.getPatFieldFull();
                if (full != null) {
                    if (!isIrrefutable(full.getPat())) return false;
                } else if (field.getPatBinding() == null) {
                    return false;
                }
            }
            return true;
        }
        if (pat instanceof RsPatTupleStruct) {
            RsPatTupleStruct patTupleStruct = (RsPatTupleStruct) pat;
            if (!isIrrefutablePath(patTupleStruct.getPath())) return false;
            for (RsPat p : patTupleStruct.getPatList()) {
                if (!isIrrefutable(p)) return false;
            }
            return true;
        }
        if (pat instanceof RsPatIdent) {
            return isIrrefutableBinding(((RsPatIdent) pat).getPatBinding());
        }
        if (pat instanceof RsPatConst) {
            RsExpr expr = ((RsPatConst) pat).getExpr();
            if (expr instanceof RsPathExpr) {
                return isIrrefutablePath(((RsPathExpr) expr).getPath());
            }
            return false;
        }
        if (pat instanceof RsPatRange) {
            return false;
        }
        if (pat instanceof RsOrPat) {
            return false;
        }
        return true;
    }

    private static boolean isIrrefutablePath(@Nonnull RsPath path) {
        PsiElement item = path.getReference() instanceof org.rust.lang.core.resolve.ref.RsPathReference
            ? org.rust.lang.core.resolve.ref.RsPathReferenceImpl.deepResolve((org.rust.lang.core.resolve.ref.RsPathReference) path.getReference())
            : (path.getReference() != null ? path.getReference().resolve() : null);
        if (item instanceof RsStructItem) return true;
        if (item instanceof RsEnumVariant) {
            return RsEnumItemUtil.getVariants(RsEnumVariantUtil.getParentEnum((RsEnumVariant) item)).size() == 1;
        }
        return false;
    }

    private static boolean isIrrefutableBinding(@Nonnull RsPatBinding binding) {
        PsiElement resolved = binding.getReference().resolve();
        if (resolved instanceof RsConstant) return false;
        if (resolved instanceof RsStructItem) return true;
        if (resolved instanceof RsEnumVariant) {
            return RsEnumItemUtil.getVariants(RsEnumVariantUtil.getParentEnum((RsEnumVariant) resolved)).size() == 1;
        }
        return true;
    }

    @Nonnull
    public static RsPat skipUnnecessaryTupDown(@Nonnull RsPat pat) {
        while (pat instanceof RsPatTup) {
            List<RsPat> patList = ((RsPatTup) pat).getPatList();
            if (patList.size() != 1) return pat;
            pat = patList.get(0);
        }
        return pat;
    }

    /**
     * Returns all visible local variable bindings at the given element.
     */
    @Nonnull
    public static Map<String, RsPatBinding> getLocalVariableVisibleBindings(@Nonnull PsiElement element) {
        return RsElementUtil.getLocalVariableVisibleBindings(element);
    }
}
