/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.openapiext.Testmark;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsNamedFieldDecl;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFieldsOwner;
import org.rust.lang.core.psi.ext.RsFieldsOwnerUtil;
import org.rust.lang.core.psi.ext.RsPatFieldUtil;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.ty.TyAdt;

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
    /** The struct a struct-pattern matches: its own path if that resolves, else the expected type's item. */
    @Nullable
    private static RsFieldsOwner structOf(
        @Nonnull RsPatStruct patStruct, @Nonnull Ty ty
    ) {
        RsPath path = patStruct.getPath();
        if (path != null && path.getReference() != null) {
            PsiElement resolved = path.getReference().resolve();
            if (resolved instanceof RsFieldsOwner) {
                return (RsFieldsOwner) resolved;
            }
        }
        if (ty instanceof TyAdt) {
            RsElement item = ((TyAdt) ty).getItem();
            if (item instanceof RsFieldsOwner) {
                return (RsFieldsOwner) item;
            }
        }
        return null;
    }

    public static void extractBindings(@Nonnull RsPat pat, @Nonnull RsTypeInferenceWalker walker, @Nonnull Ty ty) {
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
            RsPatStruct patStruct = (RsPatStruct) pat;
            // Field types come from the struct declaration, substituted with the expected type's
            // arguments. Asking for the type of the pattern field instead would re-enter inference
            // of the function being inferred right now and recurse forever.
            java.util.Map<String, RsNamedFieldDecl> declaredFields =
                new java.util.HashMap<>();
            RsFieldsOwner owner = structOf(patStruct, ty);
            if (owner != null) {
                for (RsNamedFieldDecl decl
                    : RsFieldsOwnerUtil.getNamedFields(owner)) {
                    String fieldName = decl.getName();
                    if (fieldName != null) declaredFields.put(fieldName, decl);
                }
            }
            Substitution subst = ty instanceof TyAdt
                ? ((TyAdt) ty).getTypeParameterValues()
                : SubstitutionUtil.EMPTY;

            for (RsPatField field : patStruct.getPatFieldList()) {
                String fieldName = RsPatFieldUtil.getFieldName(
                    RsPatFieldUtil.getKind(field));
                RsNamedFieldDecl decl =
                    fieldName == null ? null : declaredFields.get(fieldName);
                Ty fieldTy = TyUnknown.INSTANCE;
                if (decl != null && decl.getTypeReference() != null) {
                    fieldTy = FoldUtil.substituteOrUnknown(
                        ExtensionsUtil.getRawType(decl.getTypeReference()), subst);
                }
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
