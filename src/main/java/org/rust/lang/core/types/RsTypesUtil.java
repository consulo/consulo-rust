/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

/**
 * Bridge class delegating to {@link ExtensionsKt} and related type utility classes.
 */
public final class RsTypesUtil {
    private RsTypesUtil() {
    }

    @NotNull
    public static Ty getType(@NotNull RsExpr expr) {
        return ExtensionsUtil.getType(expr);
    }

    @NotNull
    public static Ty getType(@NotNull RsPatBinding binding) {
        return ExtensionsUtil.getType(binding);
    }

    @NotNull
    public static Ty getType(@NotNull RsPat pat) {
        return ExtensionsUtil.getType(pat);
    }

    @NotNull
    public static Ty getType(@NotNull RsPatField patField) {
        return ExtensionsUtil.getType(patField);
    }

    @NotNull
    public static Ty getType(@NotNull org.rust.lang.core.psi.RsStructLiteralField field) {
        return ExtensionsUtil.getType(field);
    }

    @NotNull
    public static RsInferenceResult getInference(@NotNull PsiElement element) {
        return ExtensionsUtil.getInference(element);
    }

    @Nullable
    public static RsInferenceResult getSelfInferenceResult(@NotNull RsInferenceContextOwner owner) {
        return ExtensionsUtil.getSelfInferenceResult(owner);
    }

    @NotNull
    public static ImplLookup getImplLookup(@NotNull RsElement element) {
        return ExtensionsUtil.getImplLookup(element);
    }

    @NotNull
    public static KnownItems getKnownItems(@NotNull RsElement element) {
        return org.rust.lang.core.resolve.KnownItems.getKnownItems(element);
    }

    @NotNull
    public static Ty getRawType(@NotNull RsTypeReference typeRef) {
        return ExtensionsUtil.getRawType(typeRef);
    }

    @NotNull
    public static Ty getNormType(@NotNull RsTypeReference typeRef) {
        return ExtensionsUtil.getNormType(typeRef);
    }

    @NotNull
    public static Ty normType(@NotNull RsTypeReference typeRef, @NotNull ImplLookup implLookup) {
        return ExtensionsUtil.normType(typeRef, implLookup);
    }

    @Nullable
    public static RsElement getDeclaration(@NotNull RsExpr expr) {
        return ExtensionsUtil.getDeclaration(expr);
    }

    @NotNull
    public static ControlFlowGraph getControlFlowGraph(@NotNull RsInferenceContextOwner owner) {
        return ExtensionsUtil.getControlFlowGraph(owner);
    }

    @NotNull
    public static Ty stripReferences(@NotNull Ty ty) {
        return TyUtil.stripReferences(ty);
    }
}
