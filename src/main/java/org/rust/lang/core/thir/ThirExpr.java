/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.MirMatch;
import org.rust.lang.core.mir.schemas.MirBorrowKind;
import org.rust.lang.core.mir.schemas.MirSpan;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.ext.BinaryOperator;
import org.rust.lang.core.psi.ext.LogicOp;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.psi.ext.UnaryOperator;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.regions.Scope;

import java.util.List;

public abstract class ThirExpr {
    @NotNull
    public final Ty ty;
    @NotNull
    public final MirSpan span;

    @Nullable
    private Scope _tempLifetime;

    protected ThirExpr(@NotNull Ty ty, @NotNull MirSpan span) {
        this.ty = ty;
        this.span = span;
    }

    @NotNull
    public Ty getTy() {
        return ty;
    }

    @NotNull
    public MirSpan getSpan() {
        return span;
    }

    @Nullable
    public Scope getTempLifetime() {
        return _tempLifetime;
    }

    public void setTempLifetime(@Nullable Scope value) {
        _tempLifetime = value;
    }

    @NotNull
    public ThirExpr withLifetime(@Nullable Scope tempLifetime) {
        this._tempLifetime = tempLifetime;
        return this;
    }

    public static class ScopeExpr extends ThirExpr {
        @NotNull public final org.rust.lang.core.types.regions.Scope regionScope;
        @NotNull public final ThirExpr expr;

        public ScopeExpr(@NotNull org.rust.lang.core.types.regions.Scope regionScope, @NotNull ThirExpr expr, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.regionScope = regionScope;
            this.expr = expr;
        }

        @NotNull
        public org.rust.lang.core.types.regions.Scope getRegionScope() {
            return regionScope;
        }

        @NotNull
        public ThirExpr getExpr() {
            return expr;
        }
    }

    public static class Literal extends ThirExpr {
        @NotNull public final RsLitExpr literal;
        public final boolean neg;

        public Literal(@NotNull RsLitExpr literal, boolean neg, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.literal = literal;
            this.neg = neg;
        }

        @NotNull
        public RsLitExpr getLiteral() {
            return literal;
        }

        public boolean getNeg() {
            return neg;
        }
    }

    public static class NonHirLiteral extends ThirExpr {
        public NonHirLiteral(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class ZstLiteral extends ThirExpr {
        public ZstLiteral(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class NamedConst extends ThirExpr {
        @NotNull public final RsConstant def;
        public NamedConst(@NotNull RsConstant def, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.def = def;
        }

        @NotNull
        public RsConstant getDef() {
            return def;
        }
    }

    public static class ConstParam extends ThirExpr {
        public ConstParam(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class StaticRef extends ThirExpr {
        public StaticRef(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class Unary extends ThirExpr {
        @NotNull public final UnaryOperator op;
        @NotNull public final ThirExpr arg;
        public Unary(@NotNull UnaryOperator op, @NotNull ThirExpr arg, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.op = op;
            this.arg = arg;
        }

        @NotNull
        public UnaryOperator getOp() {
            return op;
        }

        @NotNull
        public ThirExpr getArg() {
            return arg;
        }
    }

    public static class Binary extends ThirExpr {
        @NotNull public final BinaryOperator op;
        @NotNull public final ThirExpr left;
        @NotNull public final ThirExpr right;
        public Binary(@NotNull BinaryOperator op, @NotNull ThirExpr left, @NotNull ThirExpr right, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @NotNull
        public BinaryOperator getOp() {
            return op;
        }

        @NotNull
        public ThirExpr getLeft() {
            return left;
        }

        @NotNull
        public ThirExpr getRight() {
            return right;
        }
    }

    public static class Logical extends ThirExpr {
        @NotNull public final LogicOp op;
        @NotNull public final ThirExpr left;
        @NotNull public final ThirExpr right;
        public Logical(@NotNull LogicOp op, @NotNull ThirExpr left, @NotNull ThirExpr right, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @NotNull
        public LogicOp getOp() {
            return op;
        }

        @NotNull
        public ThirExpr getLeft() {
            return left;
        }

        @NotNull
        public ThirExpr getRight() {
            return right;
        }
    }

    public static class Block extends ThirExpr {
        @NotNull public final ThirBlock block;
        public Block(@NotNull ThirBlock block, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.block = block;
        }

        @NotNull
        public ThirBlock getBlock() {
            return block;
        }
    }

    public static class If extends ThirExpr {
        @NotNull public final Scope.IfThen ifThenScope;
        @NotNull public final ThirExpr cond;
        @NotNull public final ThirExpr then;
        @Nullable public final ThirExpr elseExpr;
        public If(@NotNull Scope.IfThen ifThenScope, @NotNull ThirExpr cond, @NotNull ThirExpr then, @Nullable ThirExpr elseExpr, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.ifThenScope = ifThenScope;
            this.cond = cond;
            this.then = then;
            this.elseExpr = elseExpr;
        }

        @NotNull
        public Scope.IfThen getIfThenScope() {
            return ifThenScope;
        }

        @NotNull
        public ThirExpr getCond() {
            return cond;
        }

        @NotNull
        public ThirExpr getThen() {
            return then;
        }

        @Nullable
        public ThirExpr getElseExpr() {
            return elseExpr;
        }
    }

    public static class Array extends ThirExpr {
        @NotNull public final List<ThirExpr> fields;
        public Array(@NotNull List<ThirExpr> fields, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.fields = fields;
        }

        @NotNull
        public List<ThirExpr> getFields() {
            return fields;
        }
    }

    public static class Repeat extends ThirExpr {
        @NotNull public final ThirExpr value;
        @NotNull public final Const count;
        public Repeat(@NotNull ThirExpr value, @NotNull Const count, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.value = value;
            this.count = count;
        }

        @NotNull
        public ThirExpr getValue() {
            return value;
        }

        @NotNull
        public Const getCount() {
            return count;
        }
    }

    public static class Tuple extends ThirExpr {
        @NotNull public final List<ThirExpr> fields;
        public Tuple(@NotNull List<ThirExpr> fields, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.fields = fields;
        }

        @NotNull
        public List<ThirExpr> getFields() {
            return fields;
        }
    }

    public static class Field extends ThirExpr {
        @NotNull public final ThirExpr expr;
        public final int fieldIndex;
        public Field(@NotNull ThirExpr expr, int fieldIndex, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.expr = expr;
            this.fieldIndex = fieldIndex;
        }

        @NotNull
        public ThirExpr getExpr() {
            return expr;
        }

        public int getFieldIndex() {
            return fieldIndex;
        }
    }

    public static class Loop extends ThirExpr {
        @NotNull public final ThirExpr body;
        public Loop(@NotNull ThirExpr body, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.body = body;
        }

        @NotNull
        public ThirExpr getBody() {
            return body;
        }
    }

    public static class NeverToAny extends ThirExpr {
        @NotNull public final ThirExpr spanExpr;
        public NeverToAny(@NotNull ThirExpr spanExpr, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.spanExpr = spanExpr;
        }

        @NotNull
        public ThirExpr getSpanExpr() {
            return spanExpr;
        }
    }

    public static class Break extends ThirExpr {
        @NotNull public final Scope label;
        @Nullable public final ThirExpr expr;
        public Break(@NotNull Scope label, @Nullable ThirExpr expr, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.label = label;
            this.expr = expr;
        }

        @NotNull
        public Scope getLabel() {
            return label;
        }

        @Nullable
        public ThirExpr getExpr() {
            return expr;
        }
    }

    public static class Continue extends ThirExpr {
        public Continue(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class Return extends ThirExpr {
        public Return(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class VarRef extends ThirExpr {
        @NotNull public final LocalVar local;
        public VarRef(@NotNull LocalVar local, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.local = local;
        }

        @NotNull
        public LocalVar getLocal() {
            return local;
        }
    }

    public static class UpvarRef extends ThirExpr {
        public UpvarRef(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class Assign extends ThirExpr {
        @NotNull public final ThirExpr left;
        @NotNull public final ThirExpr right;
        public Assign(@NotNull ThirExpr left, @NotNull ThirExpr right, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.left = left;
            this.right = right;
        }

        @NotNull
        public ThirExpr getLeft() {
            return left;
        }

        @NotNull
        public ThirExpr getRight() {
            return right;
        }
    }

    public static class AssignOp extends ThirExpr {
        @NotNull public final BinaryOperator op;
        @NotNull public final ThirExpr left;
        @NotNull public final ThirExpr right;
        public AssignOp(@NotNull BinaryOperator op, @NotNull ThirExpr left, @NotNull ThirExpr right, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @NotNull
        public BinaryOperator getOp() {
            return op;
        }

        @NotNull
        public ThirExpr getLeft() {
            return left;
        }

        @NotNull
        public ThirExpr getRight() {
            return right;
        }
    }

    public static class Adt extends ThirExpr {
        @NotNull public final RsStructOrEnumItemElement definition;
        public final int variantIndex;
        @NotNull public final List<FieldExpr> fields;
        @Nullable public final FruInfo base;
        public Adt(@NotNull RsStructOrEnumItemElement definition, int variantIndex, @NotNull List<FieldExpr> fields, @Nullable FruInfo base, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.definition = definition;
            this.variantIndex = variantIndex;
            this.fields = fields;
            this.base = base;
        }

        @NotNull
        public RsStructOrEnumItemElement getDefinition() {
            return definition;
        }

        public int getVariantIndex() {
            return variantIndex;
        }

        @NotNull
        public List<FieldExpr> getFields() {
            return fields;
        }

        @Nullable
        public FruInfo getBase() {
            return base;
        }
    }

    public static class Borrow extends ThirExpr {
        @NotNull public final MirBorrowKind kind;
        @NotNull public final ThirExpr arg;
        public Borrow(@NotNull MirBorrowKind kind, @NotNull ThirExpr arg, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.kind = kind;
            this.arg = arg;
        }

        @NotNull
        public MirBorrowKind getKind() {
            return kind;
        }

        @NotNull
        public ThirExpr getArg() {
            return arg;
        }
    }

    public static class AddressOf extends ThirExpr {
        public AddressOf(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class BoxExpr extends ThirExpr {
        public BoxExpr(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class Call extends ThirExpr {
        @NotNull public final Ty fnTy;
        @NotNull public final ThirExpr callee;
        @NotNull public final List<ThirExpr> args;
        public final boolean fromCall;
        public Call(@NotNull Ty fnTy, @NotNull ThirExpr callee, @NotNull List<ThirExpr> args, boolean fromCall, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.fnTy = fnTy;
            this.callee = callee;
            this.args = args;
            this.fromCall = fromCall;
        }

        @NotNull
        public Ty getFnTy() {
            return fnTy;
        }

        @NotNull
        public ThirExpr getCallee() {
            return callee;
        }

        @NotNull
        public List<ThirExpr> getArgs() {
            return args;
        }

        public boolean getFromCall() {
            return fromCall;
        }
    }

    public static class Deref extends ThirExpr {
        @NotNull public final ThirExpr arg;
        public Deref(@NotNull ThirExpr arg, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.arg = arg;
        }

        @NotNull
        public ThirExpr getArg() {
            return arg;
        }
    }

    public static class Cast extends ThirExpr {
        @NotNull public final ThirExpr source;
        public Cast(@NotNull ThirExpr source, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.source = source;
        }

        @NotNull
        public ThirExpr getSource() {
            return source;
        }
    }

    public static class Use extends ThirExpr {
        @NotNull public final ThirExpr source;
        public Use(@NotNull ThirExpr source, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.source = source;
        }

        @NotNull
        public ThirExpr getSource() {
            return source;
        }
    }

    public static class Pointer extends ThirExpr {
        public Pointer(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class Let extends ThirExpr {
        @NotNull public final ThirPat pat;
        @NotNull public final ThirExpr expr;
        public Let(@NotNull ThirPat pat, @NotNull ThirExpr expr, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.pat = pat;
            this.expr = expr;
        }

        @NotNull
        public ThirPat getPat() {
            return pat;
        }

        @NotNull
        public ThirExpr getExpr() {
            return expr;
        }
    }

    public static class Match extends ThirExpr {
        @NotNull public final ThirExpr expr;
        @NotNull public final List<MirMatch.MirArm> arms;
        public Match(@NotNull ThirExpr expr, @NotNull List<MirMatch.MirArm> arms, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.expr = expr;
            this.arms = arms;
        }

        @NotNull
        public ThirExpr getExpr() {
            return expr;
        }

        @NotNull
        public List<MirMatch.MirArm> getArms() {
            return arms;
        }
    }

    public static class Index extends ThirExpr {
        @NotNull public final ThirExpr lhs;
        @NotNull public final ThirExpr index;
        public Index(@NotNull ThirExpr lhs, @NotNull ThirExpr index, @NotNull Ty ty, @NotNull MirSpan span) {
            super(ty, span);
            this.lhs = lhs;
            this.index = index;
        }

        @NotNull
        public ThirExpr getLhs() {
            return lhs;
        }

        @NotNull
        public ThirExpr getIndex() {
            return index;
        }
    }

    public static class ConstBlock extends ThirExpr {
        public ConstBlock(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class PlaceTypeAscription extends ThirExpr {
        public PlaceTypeAscription(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class ValueTypeAscription extends ThirExpr {
        public ValueTypeAscription(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class Closure extends ThirExpr {
        public Closure(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class InlineAsm extends ThirExpr {
        public InlineAsm(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class OffsetOf extends ThirExpr {
        public OffsetOf(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class ThreadLocalRef extends ThirExpr {
        public ThreadLocalRef(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }

    public static class Yield extends ThirExpr {
        public Yield(@NotNull Ty ty, @NotNull MirSpan span) { super(ty, span); }
    }
}
