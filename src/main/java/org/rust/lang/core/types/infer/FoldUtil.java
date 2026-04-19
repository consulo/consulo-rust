/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.KindUtil;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.consts.CtUnknown;
import org.rust.lang.core.types.regions.ReEarlyBound;
import org.rust.lang.core.types.regions.ReUnknown;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;

import java.util.ArrayList;
import java.util.List;

public final class FoldUtil {
    private FoldUtil() {
    }

    // HasTypeFlagVisitor equivalents
    private static final TypeVisitor HAS_TY_INFER_VISITOR = new HasTypeFlagVisitor(KindUtil.HAS_TY_INFER_MASK);
    private static final TypeVisitor HAS_TY_TYPE_PARAMETER_VISITOR = new HasTypeFlagVisitor(KindUtil.HAS_TY_TYPE_PARAMETER_MASK);
    private static final TypeVisitor HAS_TY_PROJECTION_VISITOR = new HasTypeFlagVisitor(KindUtil.HAS_TY_PROJECTION_MASK);
    private static final TypeVisitor HAS_RE_EARLY_BOUND_VISITOR = new HasTypeFlagVisitor(KindUtil.HAS_RE_EARLY_BOUND_MASK);
    private static final TypeVisitor HAS_CT_INFER_VISITOR = new HasTypeFlagVisitor(KindUtil.HAS_CT_INFER_MASK);
    private static final TypeVisitor HAS_CT_PARAMETER_VISITOR = new HasTypeFlagVisitor(KindUtil.HAS_CT_PARAMETER_MASK);
    private static final TypeVisitor HAS_TY_PLACEHOLDER_VISITOR = new HasTypeFlagVisitor(KindUtil.HAS_TY_PLACEHOLDER_MASK);
    private static final TypeVisitor NEEDS_INFER = new HasTypeFlagVisitor(KindUtil.HAS_TY_INFER_MASK | KindUtil.HAS_CT_INFER_MASK);
    private static final TypeVisitor NEEDS_EVAL = new HasTypeFlagVisitor(KindUtil.HAS_CT_UNEVALUATED_MASK | KindUtil.HAS_CT_PARAMETER_MASK);
    private static final TypeVisitor NEEDS_SUBST = new HasTypeFlagVisitor(
        KindUtil.HAS_TY_TYPE_PARAMETER_MASK | KindUtil.HAS_RE_EARLY_BOUND_MASK | KindUtil.HAS_CT_PARAMETER_MASK
    );
    private static final TypeVisitor HAS_FREE_LOCAL_NAMES = new HasTypeFlagVisitor(
        KindUtil.HAS_TY_TYPE_PARAMETER_MASK | KindUtil.HAS_CT_PARAMETER_MASK
            | KindUtil.HAS_TY_INFER_MASK | KindUtil.HAS_CT_INFER_MASK | KindUtil.HAS_TY_OPAQUE_MASK
    );

    public static boolean hasTyInfer(@NotNull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_TY_INFER_VISITOR);
    }

    public static boolean hasTyTypeParameters(@NotNull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_TY_TYPE_PARAMETER_VISITOR);
    }

    public static boolean hasTyProjection(@NotNull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_TY_PROJECTION_VISITOR);
    }

    public static boolean hasReEarlyBounds(@NotNull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_RE_EARLY_BOUND_VISITOR);
    }

    public static boolean hasCtInfer(@NotNull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_CT_INFER_VISITOR);
    }

    public static boolean hasCtConstParameters(@NotNull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_CT_PARAMETER_VISITOR);
    }

    public static boolean hasTyPlaceholder(@NotNull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_TY_PLACEHOLDER_VISITOR);
    }

    public static boolean needsInfer(@NotNull TypeFoldable<?> foldable) {
        return foldable.visitWith(NEEDS_INFER);
    }

    public static boolean needsSubst(@NotNull TypeFoldable<?> foldable) {
        return foldable.visitWith(NEEDS_SUBST);
    }

    public static boolean needsEval(@NotNull TypeFoldable<?> foldable) {
        return foldable.visitWith(NEEDS_EVAL);
    }

    public static boolean isGlobal(@NotNull TypeFoldable<?> foldable) {
        return !foldable.visitWith(HAS_FREE_LOCAL_NAMES);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends TypeFoldable<T>> T substitute(@NotNull TypeFoldable<T> foldable, @NotNull Substitution subst) {
        return ConstExprEvaluator.tryEvaluate(foldable.foldWith(new TypeFolder() {
            @NotNull
            @Override
            public Ty foldTy(@NotNull Ty ty) {
                if (ty instanceof TyTypeParameter) {
                    Ty result = subst.get((TyTypeParameter) ty);
                    return result != null ? result : ty;
                }
                if (needsSubst(ty)) return ty.superFoldWith(this);
                return ty;
            }

            @NotNull
            @Override
            public Region foldRegion(@NotNull Region region) {
                if (region instanceof ReEarlyBound) {
                    Region result = subst.get((ReEarlyBound) region);
                    return result != null ? result : region;
                }
                return region;
            }

            @NotNull
            @Override
            public Const foldConst(@NotNull Const aConst) {
                if (aConst instanceof CtConstParameter) {
                    Const result = subst.get((CtConstParameter) aConst);
                    return result != null ? result : aConst;
                }
                if (hasCtConstParameters(aConst)) return aConst.superFoldWith(this);
                return aConst;
            }
        }));
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends TypeFoldable<T>> T substituteOrUnknown(@NotNull TypeFoldable<T> foldable, @NotNull Substitution subst) {
        return ConstExprEvaluator.tryEvaluate(foldable.foldWith(new TypeFolder() {
            @NotNull
            @Override
            public Ty foldTy(@NotNull Ty ty) {
                if (ty instanceof TyTypeParameter) {
                    Ty result = subst.get((TyTypeParameter) ty);
                    return result != null ? result : TyUnknown.INSTANCE;
                }
                if (needsSubst(ty)) return ty.superFoldWith(this);
                return ty;
            }

            @NotNull
            @Override
            public Region foldRegion(@NotNull Region region) {
                if (region instanceof ReEarlyBound) {
                    Region result = subst.get((ReEarlyBound) region);
                    return result != null ? result : ReUnknown.INSTANCE;
                }
                return region;
            }

            @NotNull
            @Override
            public Const foldConst(@NotNull Const aConst) {
                if (aConst instanceof CtConstParameter) {
                    Const result = subst.get((CtConstParameter) aConst);
                    return result != null ? result : CtUnknown.INSTANCE;
                }
                if (hasCtConstParameters(aConst)) return aConst.superFoldWith(this);
                return aConst;
            }
        }));
    }

    public static <T> boolean containsTyOfClass(@NotNull TypeFoldable<T> foldable, @NotNull List<Class<?>> classes) {
        return foldable.visitWith(new TypeVisitor() {
            @Override
            public boolean visitTy(@NotNull Ty ty) {
                for (Class<?> clazz : classes) {
                    if (clazz.isInstance(ty)) return true;
                }
                return ty.superVisitWith(this);
            }
        });
    }

    public static <T> boolean containsTyOfClass(@NotNull TypeFoldable<T> foldable, @NotNull Class<?>... classes) {
        return containsTyOfClass(foldable, java.util.Arrays.asList(classes));
    }

    @NotNull
    public static <T> List<TyInfer> collectInferTys(@NotNull TypeFoldable<T> foldable) {
        List<TyInfer> list = new ArrayList<>();
        visitInferTys(foldable, ty -> {
            list.add(ty);
            return false;
        });
        return list;
    }

    public static <T> boolean visitInferTys(@NotNull TypeFoldable<T> foldable, @NotNull java.util.function.Function<TyInfer, Boolean> visitor) {
        return foldable.visitWith(new TypeVisitor() {
            @Override
            public boolean visitTy(@NotNull Ty ty) {
                if (ty instanceof TyInfer) return visitor.apply((TyInfer) ty);
                if (hasTyInfer(ty)) return ty.superVisitWith(this);
                return false;
            }
        });
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <T extends TypeFoldable<T>> T foldTyPlaceholderWithTyInfer(@NotNull T foldable) {
        if (!hasTyPlaceholder(foldable)) return foldable;
        return foldable.foldWith(new TypeFolder() {
            @NotNull
            @Override
            public Ty foldTy(@NotNull Ty ty) {
                if (ty instanceof TyPlaceholder) {
                    return new TyInfer.TyVar();
                }
                if (hasTyPlaceholder(ty)) return ty.superFoldWith(this);
                return ty;
            }
        });
    }

    private static class HasTypeFlagVisitor implements TypeVisitor {
        private final int myMask;

        HasTypeFlagVisitor(int mask) {
            myMask = mask;
        }

        @Override
        public boolean visitTy(@NotNull Ty ty) {
            return (ty.getFlags() & myMask) != 0;
        }

        @Override
        public boolean visitRegion(@NotNull Region region) {
            return (region.getFlags() & myMask) != 0;
        }

        @Override
        public boolean visitConst(@NotNull Const aConst) {
            return (aConst.getFlags() & myMask) != 0;
        }
    }
}
