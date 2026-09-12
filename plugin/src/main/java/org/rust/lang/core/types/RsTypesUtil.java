/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.dfa.ControlFlowGraph;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsInferenceContextOwner;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.infer.RsInferenceResult;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyReference;
import org.rust.lang.core.types.ty.TyUtil;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.psi.RsStructLiteralField;

/**
 * Bridge class delegating to {@link ExtensionsKt} and related type utility classes.
 */
public final class RsTypesUtil {
    private RsTypesUtil() {
    }

    @Nonnull
    public static Ty getType(@Nonnull RsExpr expr) {
        return ExtensionsUtil.getType(expr);
    }

    @Nonnull
    public static Ty getType(@Nonnull RsPatBinding binding) {
        return ExtensionsUtil.getType(binding);
    }

    @Nonnull
    public static Ty getType(@Nonnull RsPat pat) {
        return ExtensionsUtil.getType(pat);
    }

    @Nonnull
    public static Ty getType(@Nonnull RsPatField patField) {
        return ExtensionsUtil.getType(patField);
    }

    @Nonnull
    public static Ty getType(@Nonnull org.rust.lang.core.psi.RsStructLiteralField field) {
        return ExtensionsUtil.getType(field);
    }

    @Nonnull
    public static RsInferenceResult getInference(@Nonnull PsiElement element) {
        return ExtensionsUtil.getInference(element);
    }

    @Nullable
    public static RsInferenceResult getSelfInferenceResult(@Nonnull RsInferenceContextOwner owner) {
        return ExtensionsUtil.getSelfInferenceResult(owner);
    }

    @Nonnull
    public static ImplLookup getImplLookup(@Nonnull RsElement element) {
        return ExtensionsUtil.getImplLookup(element);
    }

    @Nonnull
    public static KnownItems getKnownItems(@Nonnull RsElement element) {
        return org.rust.lang.core.resolve.KnownItems.getKnownItems(element);
    }

    @Nonnull
    public static Ty getRawType(@Nonnull RsTypeReference typeRef) {
        return ExtensionsUtil.getRawType(typeRef);
    }

    @Nonnull
    public static Ty getNormType(@Nonnull RsTypeReference typeRef) {
        return ExtensionsUtil.getNormType(typeRef);
    }

    @Nonnull
    public static Ty normType(@Nonnull RsTypeReference typeRef, @Nonnull ImplLookup implLookup) {
        return ExtensionsUtil.normType(typeRef, implLookup);
    }

    @Nullable
    public static RsElement getDeclaration(@Nonnull RsExpr expr) {
        return ExtensionsUtil.getDeclaration(expr);
    }

    @Nonnull
    public static ControlFlowGraph getControlFlowGraph(@Nonnull RsInferenceContextOwner owner) {
        return ExtensionsUtil.getControlFlowGraph(owner);
    }

    @Nonnull
    public static Ty stripReferences(@Nonnull Ty ty) {
        return TyUtil.stripReferences(ty);
    }
}
