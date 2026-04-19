/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.openapiext.PsiExtUtil;

import java.util.function.Consumer;

public final class RsExprExtUtil {
    private RsExprExtUtil() {}

    /** {@code val RsExpr.adjustedType: Ty} — last adjustment target, or the type itself. */
    @NotNull
    public static Ty getAdjustedType(@NotNull RsExpr expr) {
        return ExtensionsUtil.getAdjustedType(expr);
    }

    /** {@code val RsExpr.isInConstContext: Boolean} — true iff surrounded by a const context. */
    public static boolean isInConstContext(@NotNull RsExpr expr) {
        return classifyConstContext(expr) != null;
    }

    /** Mirrors {@code val RsExpr.classifyConstContext: RsConstContextKind?}. */
    @Nullable
    public static RsConstContextKind classifyConstContext(@NotNull RsExpr expr) {
        for (PsiElement it : RsElementUtil.getContexts(expr)) {
            if (it instanceof RsConstant) {
                RsConstant constant = (RsConstant) it;
                return RsConstantUtil.isConst(constant) ? new RsConstContextKind.Constant(constant) : null;
            }
            if (it instanceof RsFunction) {
                RsFunction fn = (RsFunction) it;
                return RsFunctionUtil.isConst(fn) ? new RsConstContextKind.ConstFn(fn) : null;
            }
            if (it instanceof RsVariantDiscriminant) {
                PsiElement parent = it.getParent();
                if (parent instanceof RsEnumVariant) {
                    return new RsConstContextKind.EnumVariantDiscriminant((RsEnumVariant) parent);
                }
            }
            if (it instanceof RsExpr) {
                PsiElement parent = it.getParent();
                if (parent instanceof RsArrayType && it == ((RsArrayType) parent).getExpr()) {
                    return RsConstContextKind.ArraySize;
                }
                if (parent instanceof RsArrayExpr && it == RsArrayExprUtil.getSizeExpr((RsArrayExpr) parent)) {
                    return RsConstContextKind.ArraySize;
                }
                if (parent instanceof RsTypeArgumentList) {
                    return RsConstContextKind.ConstGenericArgument;
                }
            } else if (it instanceof RsItemElement) {
                return null;
            }
        }
        return null;
    }

    /** {@code val RsExpr.isInUnsafeContext: Boolean}. */
    public static boolean isInUnsafeContext(@NotNull RsExpr expr) {
        for (PsiElement it : RsElementUtil.getContexts(expr)) {
            if (it instanceof RsBlockExpr) {
                if (RsBlockExprUtil.isUnsafe((RsBlockExpr) it)) return true;
                // continue scanning — enclosing fn could still be unsafe
            }
            if (it instanceof RsFunction) {
                return RsFunctionUtil.isActuallyUnsafe((RsFunction) it);
            }
        }
        return false;
    }

    /** {@code val RsExpr.isTailExpr: Boolean}. */
    public static boolean isTailExpr(@NotNull RsExpr expr) {
        PsiElement parent = expr.getParent();
        return parent instanceof RsExprStmt && RsStmtUtil.isTailStmt((RsExprStmt) parent);
    }

    /**
     * Invokes {@code sink} for every {@link RsBreakExpr} in the given labeled expression that
     * matches the label criteria. Simple overload retained for callers that want every break;
     * pass {@code null} label and {@code false} matchOnlyByLabel for equivalent behavior.
     */
    public static void processBreakExprs(@NotNull RsLabeledExpression labeledExpr,
                                         @NotNull Consumer<RsBreakExpr> processor) {
        processBreakExprs(labeledExpr, null, false, processor);
    }

    /** Mirrors {@code fun RsLabeledExpression.processBreakExprs(label, matchOnlyByLabel, sink)}. */
    public static void processBreakExprs(@NotNull RsLabeledExpression labeledExpr,
                                         @Nullable String label,
                                         boolean matchOnlyByLabel,
                                         @NotNull Consumer<RsBreakExpr> sink) {
        processBreakExprsInternal(labeledExpr, label, matchOnlyByLabel, sink);
    }

    private static void processBreakExprsInternal(@NotNull PsiElement element,
                                                  @Nullable String label,
                                                  boolean matchOnlyByLabel,
                                                  @NotNull Consumer<RsBreakExpr> sink) {
        PsiExtUtil.forEachChild(element, child -> {
            if (child instanceof RsBreakExpr) {
                RsBreakExpr breakExpr = (RsBreakExpr) child;
                processBreakExprsInternal(breakExpr, label, matchOnlyByLabel, sink);
                RsLabel breakLabel = breakExpr.getLabel();
                if ((!matchOnlyByLabel && breakLabel == null)
                    || (breakLabel != null && java.util.Objects.equals(breakLabel.getReferenceName(), label))) {
                    sink.accept(breakExpr);
                }
            } else if (child instanceof RsLooplikeExpr) {
                if (label != null) {
                    processBreakExprsInternal(child, label, true, sink);
                }
            } else {
                processBreakExprsInternal(child, label, matchOnlyByLabel, sink);
            }
        });
    }
}
