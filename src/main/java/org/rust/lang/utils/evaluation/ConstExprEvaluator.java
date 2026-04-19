/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.evaluation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsConstParameter;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPathType;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.consts.*;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.ty.*;

public final class ConstExprEvaluator {
    private ConstExprEvaluator() {
    }

    @NotNull
    public static Const evaluate(@NotNull RsExpr expr) {
        return evaluate(expr, ExtensionsUtil.getType(expr), PathExprResolver.getDefault());
    }

    @NotNull
    public static Const evaluate(@NotNull RsExpr expr, @NotNull Ty expectedTy) {
        return evaluate(expr, expectedTy, PathExprResolver.getDefault());
    }

    @NotNull
    public static Const evaluate(@NotNull RsExpr expr, @NotNull Ty expectedTy, @Nullable PathExprResolver resolver) {
        ConstExpr<? extends Ty> constExpr = ConstExprBuilder.toConstExpr(expr, expectedTy, resolver);
        if (constExpr == null) return CtUnknown.INSTANCE;
        ConstExpr<? extends Ty> evaluated = evaluateExpr(constExpr);
        return ConstExpr.toConst(evaluated);
    }

    @NotNull
    public static Const toConst(@NotNull RsElement element, @NotNull Ty expectedTy) {
        return toConst(element, expectedTy, PathExprResolver.getDefault());
    }

    @NotNull
    public static Const toConst(@NotNull RsElement element, @NotNull Ty expectedTy, @Nullable PathExprResolver resolver) {
        if (element instanceof RsExpr) {
            return evaluate((RsExpr) element, expectedTy, resolver);
        } else if (element instanceof RsPathType) {
            RsPathType pathType = (RsPathType) element;
            if (pathType.getPath() == null || pathType.getPath().getReference() == null) return CtUnknown.INSTANCE;
            Object resolved = pathType.getPath().getReference().resolve();
            if (resolved instanceof RsConstParameter) {
                return new CtConstParameter((RsConstParameter) resolved);
            } else if (resolved instanceof RsConstant) {
                RsConstant constant = (RsConstant) resolved;
                if (RsConstantUtil.isConst(constant)) {
                    Ty type = constant.getTypeReference() != null
                        ? ExtensionsUtil.getNormType(constant.getTypeReference())
                        : TyUnknown.INSTANCE;
                    RsExpr constExpr = constant.getExpr();
                    if (constExpr != null) return evaluate(constExpr, type, resolver);
                }
            }
        }
        return CtUnknown.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static <T extends Ty> ConstExpr<T> evaluateExpr(@NotNull ConstExpr<T> expr) {
        Ty expectedTy = expr.getExpectedTy();
        if (expectedTy instanceof TyBool) {
            return (ConstExpr<T>) simplifyToBool((ConstExpr<TyBool>) expr);
        } else if (expectedTy instanceof TyInteger) {
            return (ConstExpr<T>) simplifyToInteger((ConstExpr<TyInteger>) expr);
        }
        return expr;
    }

    @NotNull
    public static <T> T tryEvaluate(@NotNull TypeFoldable<T> foldable) {
        return foldable.foldWith(new TypeFolder() {
            @NotNull
            @Override
            public Ty foldTy(@NotNull Ty ty) {
                if (FoldUtil.needsEval(ty)) {
                    return ty.superFoldWith(this);
                }
                return ty;
            }

            @NotNull
            @Override
            public Const foldConst(@NotNull Const constValue) {
                if (constValue instanceof CtUnevaluated && FoldUtil.needsEval(constValue)) {
                    ConstExpr<? extends Ty> evaluated = evaluateExpr(((CtUnevaluated) constValue).getExpr());
                    return ConstExpr.toConst(evaluated);
                }
                return constValue;
            }
        });
    }

    // ---- Bool simplification ----

    @SuppressWarnings("unchecked")
    @NotNull
    private static <T extends Ty> ConstExpr<T> simplifyToBool(@NotNull ConstExpr<T> expr) {
        boolean value;
        if (expr instanceof ConstExpr.Constant) {
            ConstExpr.Constant<T> constant = (ConstExpr.Constant<T>) expr;
            Const c = constant.getConst();
            if (!(c instanceof CtValue)) return expr;
            ConstExpr<?> inner = ((CtValue) c).getExpr();
            if (!(inner instanceof ConstExpr.Value.Bool)) return expr;
            value = ((ConstExpr.Value.Bool) inner).getValue();
        } else if (expr instanceof ConstExpr.Unary) {
            ConstExpr.Unary<T> unary = (ConstExpr.Unary<T>) expr;
            if (unary.getOperator() != UnaryOperator.NOT) return new ConstExpr.Error<>();
            ConstExpr<T> result = simplifyToBool(unary.getExpr());
            if (result instanceof ConstExpr.Value.Bool) {
                value = !((ConstExpr.Value.Bool) result).getValue();
            } else if (result instanceof ConstExpr.Error) {
                return new ConstExpr.Error<>();
            } else {
                return new ConstExpr.Unary<>(unary.getOperator(), result, unary.getExpectedTy());
            }
        } else if (expr instanceof ConstExpr.Binary) {
            ConstExpr.Binary<T> binary = (ConstExpr.Binary<T>) expr;
            ConstExpr<T> left = simplifyToBool(binary.getLeft());
            ConstExpr<T> right = simplifyToBool(binary.getRight());
            BinaryOperator op = binary.getOperator();

            if (op == LogicOp.AND) {
                if (left instanceof ConstExpr.Value.Bool && !((ConstExpr.Value.Bool) left).getValue()) {
                    value = false;
                } else if (right instanceof ConstExpr.Value.Bool && !((ConstExpr.Value.Bool) right).getValue()) {
                    value = false;
                } else if (left instanceof ConstExpr.Value.Bool && right instanceof ConstExpr.Value.Bool) {
                    value = ((ConstExpr.Value.Bool) left).getValue() && ((ConstExpr.Value.Bool) right).getValue();
                } else if (left instanceof ConstExpr.Error || right instanceof ConstExpr.Error) {
                    return new ConstExpr.Error<>();
                } else {
                    return new ConstExpr.Binary<>(left, op, right, binary.getExpectedTy());
                }
            } else if (op == LogicOp.OR) {
                if (left instanceof ConstExpr.Value.Bool && ((ConstExpr.Value.Bool) left).getValue()) {
                    value = true;
                } else if (right instanceof ConstExpr.Value.Bool && ((ConstExpr.Value.Bool) right).getValue()) {
                    value = true;
                } else if (left instanceof ConstExpr.Value.Bool && right instanceof ConstExpr.Value.Bool) {
                    value = ((ConstExpr.Value.Bool) left).getValue() || ((ConstExpr.Value.Bool) right).getValue();
                } else if (left instanceof ConstExpr.Error || right instanceof ConstExpr.Error) {
                    return new ConstExpr.Error<>();
                } else {
                    return new ConstExpr.Binary<>(left, op, right, binary.getExpectedTy());
                }
            } else if (op == ArithmeticOp.BIT_XOR) {
                if (left instanceof ConstExpr.Value.Bool && right instanceof ConstExpr.Value.Bool) {
                    value = ((ConstExpr.Value.Bool) left).getValue() ^ ((ConstExpr.Value.Bool) right).getValue();
                } else if (left instanceof ConstExpr.Error || right instanceof ConstExpr.Error) {
                    return new ConstExpr.Error<>();
                } else {
                    return new ConstExpr.Binary<>(left, op, right, binary.getExpectedTy());
                }
            } else {
                return new ConstExpr.Error<>();
            }
        } else {
            return expr;
        }
        return (ConstExpr<T>) new ConstExpr.Value.Bool(value);
    }

    // ---- Integer simplification ----

    @SuppressWarnings("unchecked")
    @NotNull
    private static <T extends Ty> ConstExpr<T> simplifyToInteger(@NotNull ConstExpr<T> expr) {
        Ty expectedTy = expr.getExpectedTy();
        if (!(expectedTy instanceof TyInteger)) return new ConstExpr.Error<>();

        TyInteger intTy = (TyInteger) expectedTy;
        Long value;

        if (expr instanceof ConstExpr.Constant) {
            ConstExpr.Constant<T> constant = (ConstExpr.Constant<T>) expr;
            Const c = constant.getConst();
            if (!(c instanceof CtValue)) return expr;
            ConstExpr<?> inner = ((CtValue) c).getExpr();
            if (!(inner instanceof ConstExpr.Value.Integer)) return expr;
            value = ((ConstExpr.Value.Integer) inner).getValue();
        } else if (expr instanceof ConstExpr.Unary) {
            ConstExpr.Unary<T> unary = (ConstExpr.Unary<T>) expr;
            if (unary.getOperator() != UnaryOperator.MINUS) return new ConstExpr.Error<>();
            ConstExpr<T> result = simplifyToInteger(unary.getExpr());
            if (result instanceof ConstExpr.Value.Integer) {
                value = -((ConstExpr.Value.Integer) result).getValue();
            } else if (result instanceof ConstExpr.Error) {
                return new ConstExpr.Error<>();
            } else {
                return new ConstExpr.Unary<>(unary.getOperator(), result, unary.getExpectedTy());
            }
        } else if (expr instanceof ConstExpr.Binary) {
            ConstExpr.Binary<T> binary = (ConstExpr.Binary<T>) expr;
            ConstExpr<T> left = simplifyToInteger(binary.getLeft());
            ConstExpr<T> right = simplifyToInteger(binary.getRight());

            if (left instanceof ConstExpr.Value.Integer && right instanceof ConstExpr.Value.Integer) {
                long lv = ((ConstExpr.Value.Integer) left).getValue();
                long rv = ((ConstExpr.Value.Integer) right).getValue();
                BinaryOperator op = binary.getOperator();
                if (op == ArithmeticOp.ADD) value = lv + rv;
                else if (op == ArithmeticOp.SUB) value = lv - rv;
                else if (op == ArithmeticOp.MUL) value = lv * rv;
                else if (op == ArithmeticOp.DIV) value = rv == 0 ? null : lv / rv;
                else if (op == ArithmeticOp.REM) value = rv == 0 ? null : lv % rv;
                else if (op == ArithmeticOp.BIT_AND) value = lv & rv;
                else if (op == ArithmeticOp.BIT_OR) value = lv | rv;
                else if (op == ArithmeticOp.BIT_XOR) value = lv ^ rv;
                else if (op == ArithmeticOp.SHL) value = rv >= Long.BYTES ? null : lv << (int) rv;
                else if (op == ArithmeticOp.SHR) value = rv >= Long.BYTES ? 0L : lv >> (int) rv;
                else return new ConstExpr.Error<>();
            } else if (left instanceof ConstExpr.Error || right instanceof ConstExpr.Error) {
                return new ConstExpr.Error<>();
            } else {
                return new ConstExpr.Binary<>(left, binary.getOperator(), right, binary.getExpectedTy());
            }
        } else {
            return expr;
        }

        if (value == null || !isValidValue(value, intTy)) return new ConstExpr.Error<>();
        return (ConstExpr<T>) new ConstExpr.Value.Integer(value, intTy);
    }

    // ---- validValuesRange ----

    public static long[] getValidValuesRange(@NotNull TyInteger ty) {
        if (ty instanceof TyInteger.U8) return new long[]{0, (1L << 8) - 1};
        if (ty instanceof TyInteger.U16) return new long[]{0, (1L << 16) - 1};
        if (ty instanceof TyInteger.U32) return new long[]{0, (1L << 32) - 1};
        if (ty instanceof TyInteger.U64) return new long[]{0, Long.MAX_VALUE};
        if (ty instanceof TyInteger.U128) return new long[]{0, Long.MAX_VALUE};
        if (ty instanceof TyInteger.USize) return new long[]{0, Long.MAX_VALUE};
        if (ty instanceof TyInteger.I8) return new long[]{-(1L << 7), (1L << 7) - 1};
        if (ty instanceof TyInteger.I16) return new long[]{-(1L << 15), (1L << 15) - 1};
        if (ty instanceof TyInteger.I32) return new long[]{-(1L << 31), (1L << 31) - 1};
        if (ty instanceof TyInteger.I64) return new long[]{Long.MIN_VALUE, Long.MAX_VALUE};
        if (ty instanceof TyInteger.I128) return new long[]{Long.MIN_VALUE, Long.MAX_VALUE};
        if (ty instanceof TyInteger.ISize) return new long[]{Long.MIN_VALUE, Long.MAX_VALUE};
        return new long[]{Long.MIN_VALUE, Long.MAX_VALUE};
    }

    private static boolean isValidValue(long value, @NotNull TyInteger ty) {
        long[] range = getValidValuesRange(ty);
        return value >= range[0] && value <= range[1];
    }
}
