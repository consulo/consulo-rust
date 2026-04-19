/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;

public final class ConstExprBuilder {
    private static final int MAX_EXPR_DEPTH = 64;
    private static final TyReference STR_REF_TYPE = new TyReference(TyStr.INSTANCE, Mutability.IMMUTABLE);

    private ConstExprBuilder() {
    }

    @Nullable
    public static ConstExpr<? extends Ty> toConstExpr(@NotNull RsExpr expr) {
        return toConstExpr(expr, ExtensionsUtil.getType(expr), PathExprResolver.getDefault());
    }

    @Nullable
    public static ConstExpr<? extends Ty> toConstExpr(@NotNull RsExpr expr, @NotNull Ty expectedTy) {
        return toConstExpr(expr, expectedTy, PathExprResolver.getDefault());
    }

    @Nullable
    public static ConstExpr<? extends Ty> toConstExpr(
        @NotNull RsExpr expr,
        @NotNull Ty expectedTy,
        @Nullable PathExprResolver resolver
    ) {
        if (expectedTy instanceof TyInteger) {
            return new IntegerBuilder((TyInteger) expectedTy, resolver).build(expr, 0);
        } else if (expectedTy instanceof TyBool) {
            return new BoolBuilder(resolver).build(expr, 0);
        } else if (expectedTy instanceof TyFloat) {
            return new FloatBuilder((TyFloat) expectedTy, resolver).build(expr, 0);
        } else if (expectedTy instanceof TyChar) {
            return new CharBuilder(resolver).build(expr, 0);
        } else if (expectedTy.equals(STR_REF_TYPE)) {
            return new StrBuilder(resolver).build(expr, 0);
        }
        return null;
    }

    // ---- Abstract base ----

    private static abstract class BaseBuilder<T extends Ty> {
        @NotNull
        protected final T myExpectedTy;
        @Nullable
        protected final PathExprResolver myResolver;

        BaseBuilder(@NotNull T expectedTy, @Nullable PathExprResolver resolver) {
            myExpectedTy = expectedTy;
            myResolver = resolver;
        }

        @NotNull
        protected abstract BaseBuilder<T> copyWithDefaultResolver();

        @Nullable
        protected ConstExpr<T> makeLeafParameter(@NotNull RsConstParameter parameter) {
            return new ConstExpr.Constant<>(new CtConstParameter(parameter), myExpectedTy);
        }

        @Nullable
        ConstExpr<T> build(@Nullable RsExpr expr, int depth) {
            if (depth >= MAX_EXPR_DEPTH) return null;
            return buildInner(expr, depth);
        }

        @Nullable
        protected ConstExpr<T> buildInner(@Nullable RsExpr expr, int depth) {
            if (expr instanceof RsLitExpr) {
                return makeLeafValue((RsLitExpr) expr);
            } else if (expr instanceof RsParenExpr) {
                return build(((RsParenExpr) expr).getExpr(), depth + 1);
            } else if (expr instanceof RsBlockExpr) {
                return build(RsBlockExprUtil.getExpandedTailExpr(((RsBlockExpr) expr).getBlock()), depth + 1);
            } else if (expr instanceof RsPathExpr) {
                return buildPathExpr((RsPathExpr) expr, depth);
            }
            return null;
        }

        @Nullable
        protected abstract ConstExpr<T> makeLeafValue(@NotNull RsLitExpr expr);

        @Nullable
        private ConstExpr<T> buildPathExpr(@NotNull RsPathExpr expr, int depth) {
            if (myResolver == null) return null;
            Object element = myResolver.invoke(expr);

            RsTypeReference typeReference = null;
            if (element instanceof RsConstant) {
                RsConstant constant = (RsConstant) element;
                if (RsConstantUtil.isConst(constant)) {
                    typeReference = constant.getTypeReference();
                }
            } else if (element instanceof RsConstParameter) {
                typeReference = ((RsConstParameter) element).getTypeReference();
            }

            if (typeReference == null) return null;
            RsTypeReference skipped = RsTypeReferenceUtil.skipParens(typeReference);
            if (!(skipped instanceof RsPathType)) return null;
            RsPath typeElementPath = ((RsPathType) skipped).getPath();
            if (typeElementPath == null) return null;
            if (!myExpectedTy.equals(TyPrimitive.fromPath(typeElementPath))) return null;

            if (element instanceof RsConstant) {
                return copyWithDefaultResolver().build(((RsConstant) element).getExpr(), depth + 1);
            } else if (element instanceof RsConstParameter) {
                return makeLeafParameter((RsConstParameter) element);
            }
            return null;
        }
    }

    // ---- Integer ----

    private static class IntegerBuilder extends BaseBuilder<TyInteger> {
        IntegerBuilder(@NotNull TyInteger expectedTy, @Nullable PathExprResolver resolver) {
            super(expectedTy, resolver);
        }

        @NotNull
        @Override
        protected BaseBuilder<TyInteger> copyWithDefaultResolver() {
            return new IntegerBuilder(myExpectedTy, PathExprResolver.getDefault());
        }

        @Nullable
        @Override
        protected ConstExpr<TyInteger> makeLeafValue(@NotNull RsLitExpr expr) {
            Long val = RsLitExprUtil.getIntegerValue(expr);
            return val != null ? new ConstExpr.Value.Integer(val, myExpectedTy) : null;
        }

        @Nullable
        @Override
        protected ConstExpr<TyInteger> buildInner(@Nullable RsExpr expr, int depth) {
            if (expr instanceof RsUnaryExpr) {
                RsUnaryExpr unary = (RsUnaryExpr) expr;
                if (RsUnaryExprUtil.getOperatorType(unary) != UnaryOperator.MINUS) return null;
                ConstExpr<TyInteger> value = build(unary.getExpr(), depth + 1);
                if (value == null) return null;
                return new ConstExpr.Unary<>(UnaryOperator.MINUS, value, myExpectedTy);
            } else if (expr instanceof RsBinaryExpr) {
                RsBinaryExpr binary = (RsBinaryExpr) expr;
                Object op = RsBinaryExprUtil.getOperatorType(binary);
                if (!(op instanceof ArithmeticOp)) return null;
                ConstExpr<TyInteger> lhs = build(binary.getLeft(), depth + 1);
                if (lhs == null) return null;
                ConstExpr<TyInteger> rhs = build(binary.getRight(), depth + 1);
                if (rhs == null) return null;
                return new ConstExpr.Binary<>(lhs, (ArithmeticOp) op, rhs, myExpectedTy);
            }
            return super.buildInner(expr, depth);
        }
    }

    // ---- Bool ----

    private static class BoolBuilder extends BaseBuilder<TyBool> {
        BoolBuilder(@Nullable PathExprResolver resolver) {
            super(TyBool.INSTANCE, resolver);
        }

        @NotNull
        @Override
        protected BaseBuilder<TyBool> copyWithDefaultResolver() {
            return new BoolBuilder(PathExprResolver.getDefault());
        }

        @Nullable
        @Override
        protected ConstExpr<TyBool> makeLeafValue(@NotNull RsLitExpr expr) {
            Boolean val = RsLitExprUtil.getBooleanValue(expr);
            return val != null ? new ConstExpr.Value.Bool(val) : null;
        }

        @Nullable
        @Override
        protected ConstExpr<TyBool> buildInner(@Nullable RsExpr expr, int depth) {
            if (expr instanceof RsBinaryExpr) {
                RsBinaryExpr binary = (RsBinaryExpr) expr;
                Object op = RsBinaryExprUtil.getOperatorType(binary);
                if (op == LogicOp.AND) {
                    ConstExpr<TyBool> lhs = build(binary.getLeft(), depth + 1);
                    if (lhs == null) return null;
                    ConstExpr<TyBool> rhs = build(binary.getRight(), depth + 1);
                    if (rhs == null) rhs = new ConstExpr.Error<>();
                    return new ConstExpr.Binary<>(lhs, LogicOp.AND, rhs, myExpectedTy);
                } else if (op == LogicOp.OR) {
                    ConstExpr<TyBool> lhs = build(binary.getLeft(), depth + 1);
                    if (lhs == null) return null;
                    ConstExpr<TyBool> rhs = build(binary.getRight(), depth + 1);
                    if (rhs == null) rhs = new ConstExpr.Error<>();
                    return new ConstExpr.Binary<>(lhs, LogicOp.OR, rhs, myExpectedTy);
                } else if (op == ArithmeticOp.BIT_XOR) {
                    ConstExpr<TyBool> lhs = build(binary.getLeft(), depth + 1);
                    if (lhs == null) return null;
                    ConstExpr<TyBool> rhs = build(binary.getRight(), depth + 1);
                    if (rhs == null) return null;
                    return new ConstExpr.Binary<>(lhs, ArithmeticOp.BIT_XOR, rhs, myExpectedTy);
                }
                return null;
            } else if (expr instanceof RsUnaryExpr) {
                RsUnaryExpr unary = (RsUnaryExpr) expr;
                if (RsUnaryExprUtil.getOperatorType(unary) != UnaryOperator.NOT) return null;
                ConstExpr<TyBool> value = build(unary.getExpr(), depth + 1);
                if (value == null) return null;
                return new ConstExpr.Unary<>(UnaryOperator.NOT, value, myExpectedTy);
            }
            return super.buildInner(expr, depth);
        }
    }

    // ---- Float ----

    private static class FloatBuilder extends BaseBuilder<TyFloat> {
        FloatBuilder(@NotNull TyFloat expectedTy, @Nullable PathExprResolver resolver) {
            super(expectedTy, resolver);
        }

        @NotNull
        @Override
        protected BaseBuilder<TyFloat> copyWithDefaultResolver() {
            return new FloatBuilder(myExpectedTy, PathExprResolver.getDefault());
        }

        @Nullable
        @Override
        protected ConstExpr<TyFloat> makeLeafValue(@NotNull RsLitExpr expr) {
            Double val = RsLitExprUtil.getFloatValue(expr);
            return val != null ? new ConstExpr.Value.Float(val, myExpectedTy) : null;
        }
    }

    // ---- Char ----

    private static class CharBuilder extends BaseBuilder<TyChar> {
        CharBuilder(@Nullable PathExprResolver resolver) {
            super(TyChar.INSTANCE, resolver);
        }

        @NotNull
        @Override
        protected BaseBuilder<TyChar> copyWithDefaultResolver() {
            return new CharBuilder(PathExprResolver.getDefault());
        }

        @Nullable
        @Override
        protected ConstExpr<TyChar> makeLeafValue(@NotNull RsLitExpr expr) {
            String val = RsLitExprUtil.getCharValue(expr);
            return val != null ? new ConstExpr.Value.Char(val) : null;
        }
    }

    // ---- Str ----

    private static class StrBuilder extends BaseBuilder<TyReference> {
        StrBuilder(@Nullable PathExprResolver resolver) {
            super(STR_REF_TYPE, resolver);
        }

        @NotNull
        @Override
        protected BaseBuilder<TyReference> copyWithDefaultResolver() {
            return new StrBuilder(PathExprResolver.getDefault());
        }

        @Nullable
        @Override
        protected ConstExpr<TyReference> makeLeafValue(@NotNull RsLitExpr expr) {
            String val = RsLitExprUtil.getStringValue(expr);
            return val != null ? new ConstExpr.Value.Str(val, myExpectedTy) : null;
        }
    }
}
