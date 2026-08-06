/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
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

    public static boolean hasTyInfer(@Nonnull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_TY_INFER_VISITOR);
    }

    public static boolean hasTyTypeParameters(@Nonnull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_TY_TYPE_PARAMETER_VISITOR);
    }

    public static boolean hasTyProjection(@Nonnull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_TY_PROJECTION_VISITOR);
    }

    public static boolean hasReEarlyBounds(@Nonnull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_RE_EARLY_BOUND_VISITOR);
    }

    public static boolean hasCtInfer(@Nonnull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_CT_INFER_VISITOR);
    }

    public static boolean hasCtConstParameters(@Nonnull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_CT_PARAMETER_VISITOR);
    }

    public static boolean hasTyPlaceholder(@Nonnull TypeFoldable<?> foldable) {
        return foldable.visitWith(HAS_TY_PLACEHOLDER_VISITOR);
    }

    public static boolean needsInfer(@Nonnull TypeFoldable<?> foldable) {
        return foldable.visitWith(NEEDS_INFER);
    }

    public static boolean needsSubst(@Nonnull TypeFoldable<?> foldable) {
        return foldable.visitWith(NEEDS_SUBST);
    }

    public static boolean needsEval(@Nonnull TypeFoldable<?> foldable) {
        return foldable.visitWith(NEEDS_EVAL);
    }

    public static boolean isGlobal(@Nonnull TypeFoldable<?> foldable) {
        return !foldable.visitWith(HAS_FREE_LOCAL_NAMES);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public static <T extends TypeFoldable<T>> T substitute(@Nonnull TypeFoldable<T> foldable, @Nonnull Substitution subst) {
        return ConstExprEvaluator.tryEvaluate(foldable.foldWith(new TypeFolder() {
            @Nonnull
            @Override
            public Ty foldTy(@Nonnull Ty ty) {
                if (ty instanceof TyTypeParameter) {
                    Ty result = subst.get((TyTypeParameter) ty);
                    return result != null ? result : ty;
                }
                if (needsSubst(ty)) return ty.superFoldWith(this);
                return ty;
            }

            @Nonnull
            @Override
            public Region foldRegion(@Nonnull Region region) {
                if (region instanceof ReEarlyBound) {
                    Region result = subst.get((ReEarlyBound) region);
                    return result != null ? result : region;
                }
                return region;
            }

            @Nonnull
            @Override
            public Const foldConst(@Nonnull Const aConst) {
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
    @Nonnull
    public static <T extends TypeFoldable<T>> T substituteOrUnknown(@Nonnull TypeFoldable<T> foldable, @Nonnull Substitution subst) {
        return ConstExprEvaluator.tryEvaluate(foldable.foldWith(new TypeFolder() {
            @Nonnull
            @Override
            public Ty foldTy(@Nonnull Ty ty) {
                if (ty instanceof TyTypeParameter) {
                    Ty result = subst.get((TyTypeParameter) ty);
                    return result != null ? result : TyUnknown.INSTANCE;
                }
                if (needsSubst(ty)) return ty.superFoldWith(this);
                return ty;
            }

            @Nonnull
            @Override
            public Region foldRegion(@Nonnull Region region) {
                if (region instanceof ReEarlyBound) {
                    Region result = subst.get((ReEarlyBound) region);
                    return result != null ? result : ReUnknown.INSTANCE;
                }
                return region;
            }

            @Nonnull
            @Override
            public Const foldConst(@Nonnull Const aConst) {
                if (aConst instanceof CtConstParameter) {
                    Const result = subst.get((CtConstParameter) aConst);
                    return result != null ? result : CtUnknown.INSTANCE;
                }
                if (hasCtConstParameters(aConst)) return aConst.superFoldWith(this);
                return aConst;
            }
        }));
    }

    public static <T> boolean containsTyOfClass(@Nonnull TypeFoldable<T> foldable, @Nonnull List<Class<?>> classes) {
        return foldable.visitWith(new TypeVisitor() {
            @Override
            public boolean visitTy(@Nonnull Ty ty) {
                for (Class<?> clazz : classes) {
                    if (clazz.isInstance(ty)) return true;
                }
                return ty.superVisitWith(this);
            }
        });
    }

    public static <T> boolean containsTyOfClass(@Nonnull TypeFoldable<T> foldable, @Nonnull Class<?>... classes) {
        return containsTyOfClass(foldable, java.util.Arrays.asList(classes));
    }

    @Nonnull
    public static <T> List<TyInfer> collectInferTys(@Nonnull TypeFoldable<T> foldable) {
        List<TyInfer> list = new ArrayList<>();
        visitInferTys(foldable, ty -> {
            list.add(ty);
            return false;
        });
        return list;
    }

    public static <T> boolean visitInferTys(@Nonnull TypeFoldable<T> foldable, @Nonnull java.util.function.Function<TyInfer, Boolean> visitor) {
        return foldable.visitWith(new TypeVisitor() {
            @Override
            public boolean visitTy(@Nonnull Ty ty) {
                if (ty instanceof TyInfer) return visitor.apply((TyInfer) ty);
                if (hasTyInfer(ty)) return ty.superVisitWith(this);
                return false;
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public static <T extends TypeFoldable<T>> T foldTyPlaceholderWithTyInfer(@Nonnull T foldable) {
        if (!hasTyPlaceholder(foldable)) return foldable;
        return foldable.foldWith(new TypeFolder() {
            @Nonnull
            @Override
            public Ty foldTy(@Nonnull Ty ty) {
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
        public boolean visitTy(@Nonnull Ty ty) {
            return (ty.getFlags() & myMask) != 0;
        }

        @Override
        public boolean visitRegion(@Nonnull Region region) {
            return (region.getFlags() & myMask) != 0;
        }

        @Override
        public boolean visitConst(@Nonnull Const aConst) {
            return (aConst.getFlags() & myMask) != 0;
        }
    }
}
