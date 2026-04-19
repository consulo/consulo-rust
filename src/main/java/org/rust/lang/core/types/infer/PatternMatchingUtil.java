/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.openapiext.Testmark;

/**
 * Pattern matching utility methods and test marks.
 * Provides extractBindings which walks patterns and records binding types.
 */
public final class PatternMatchingUtil {
    private PatternMatchingUtil() {
    }

    /**
     * Walks a pattern and records the types for all bindings found within.
     */
    public static void extractBindings(@NotNull RsPat pat, @NotNull RsTypeInferenceWalker walker, @NotNull Ty ty) {
        walker.writePatTy(pat, ty);
        if (pat instanceof RsPatIdent) {
            RsPatIdent patIdent = (RsPatIdent) pat;
            // RsPatBinding is not RsPat, so we record the type on the RsPatIdent itself
            RsPat subPat = patIdent.getPat();
            if (subPat != null) {
                extractBindings(subPat, walker, ty);
            }
        } else if (pat instanceof RsPatTup) {
            // Tuple patterns: match each sub-pattern
            java.util.List<RsPat> subPats = ((RsPatTup) pat).getPatList();
            for (RsPat subPat : subPats) {
                extractBindings(subPat, walker, TyUnknown.INSTANCE);
            }
        } else if (pat instanceof RsPatStruct) {
            for (RsPatField field : ((RsPatStruct) pat).getPatFieldList()) {
                Ty fieldTy = ExtensionsUtil.getType(field);
                walker.writePatFieldTy(field, fieldTy);
                // RsPatBinding is not RsPat, so we don't call writePatTy for it
                RsPatFieldFull full = field.getPatFieldFull();
                if (full != null && full.getPat() != null) {
                    extractBindings(full.getPat(), walker, fieldTy);
                }
            }
        } else if (pat instanceof RsPatTupleStruct) {
            java.util.List<RsPat> subPats = ((RsPatTupleStruct) pat).getPatList();
            for (RsPat subPat : subPats) {
                extractBindings(subPat, walker, TyUnknown.INSTANCE);
            }
        } else if (pat instanceof RsPatSlice) {
            for (RsPat subPat : ((RsPatSlice) pat).getPatList()) {
                extractBindings(subPat, walker, TyUnknown.INSTANCE);
            }
        } else if (pat instanceof RsPatRef) {
            RsPat subPat = ((RsPatRef) pat).getPat();
            if (subPat != null) {
                extractBindings(subPat, walker, TyUnknown.INSTANCE);
            }
        } else if (pat instanceof RsOrPat) {
            for (RsPat subPat : ((RsOrPat) pat).getPatList()) {
                extractBindings(subPat, walker, ty);
            }
        }
        // RsPatWild, RsPatConst, RsPatRange, RsPatRest, RsPatMacro - no bindings to extract
    }

    public static final class PatternMatchingTestMarks {
        public static final Testmark MultipleRestPats = new Testmark();
        public static final Testmark NegativeRestSize = new Testmark();
    }
}
