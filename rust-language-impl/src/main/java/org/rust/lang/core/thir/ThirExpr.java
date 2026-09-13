/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.schemas.MirMatch;
import org.rust.lang.core.mir.schemas.MirBorrowKind;
import org.rust.lang.core.mir.schemas.MirSpan;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.ext.impl.BinaryOperator;
import org.rust.lang.core.psi.ext.impl.LogicOp;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.psi.ext.impl.UnaryOperator;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.regions.Scope;

import java.util.List;

public abstract class ThirExpr {
    @Nonnull
    public final Ty ty;
    @Nonnull
    public final MirSpan span;

    @Nullable
    private Scope _tempLifetime;

    protected ThirExpr(@Nonnull Ty ty, @Nonnull MirSpan span) {
        this.ty = ty;
        this.span = span;
    }

    @Nonnull
    public Ty getTy() {
        return ty;
    }

    @Nonnull
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

    @Nonnull
    public ThirExpr withLifetime(@Nullable Scope tempLifetime) {
        this._tempLifetime = tempLifetime;
        return this;
    }

    public static class ScopeExpr extends ThirExpr {
        @Nonnull public final org.rust.lang.core.types.regions.Scope regionScope;
        @Nonnull public final ThirExpr expr;

        public ScopeExpr(@Nonnull org.rust.lang.core.types.regions.Scope regionScope, @Nonnull ThirExpr expr, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.regionScope = regionScope;
            this.expr = expr;
        }

        @Nonnull
        public org.rust.lang.core.types.regions.Scope getRegionScope() {
            return regionScope;
        }

        @Nonnull
        public ThirExpr getExpr() {
            return expr;
        }
    }

    public static class Literal extends ThirExpr {
        @Nonnull public final RsLitExpr literal;
        public final boolean neg;

        public Literal(@Nonnull RsLitExpr literal, boolean neg, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.literal = literal;
            this.neg = neg;
        }

        @Nonnull
        public RsLitExpr getLiteral() {
            return literal;
        }

        public boolean getNeg() {
            return neg;
        }
    }

    public static class NonHirLiteral extends ThirExpr {
        public NonHirLiteral(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class ZstLiteral extends ThirExpr {
        public ZstLiteral(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class NamedConst extends ThirExpr {
        @Nonnull public final RsConstant def;
        public NamedConst(@Nonnull RsConstant def, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.def = def;
        }

        @Nonnull
        public RsConstant getDef() {
            return def;
        }
    }

    public static class ConstParam extends ThirExpr {
        public ConstParam(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class StaticRef extends ThirExpr {
        public StaticRef(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class Unary extends ThirExpr {
        @Nonnull public final UnaryOperator op;
        @Nonnull public final ThirExpr arg;
        public Unary(@Nonnull UnaryOperator op, @Nonnull ThirExpr arg, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.op = op;
            this.arg = arg;
        }

        @Nonnull
        public UnaryOperator getOp() {
            return op;
        }

        @Nonnull
        public ThirExpr getArg() {
            return arg;
        }
    }

    public static class Binary extends ThirExpr {
        @Nonnull public final BinaryOperator op;
        @Nonnull public final ThirExpr left;
        @Nonnull public final ThirExpr right;
        public Binary(@Nonnull BinaryOperator op, @Nonnull ThirExpr left, @Nonnull ThirExpr right, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Nonnull
        public BinaryOperator getOp() {
            return op;
        }

        @Nonnull
        public ThirExpr getLeft() {
            return left;
        }

        @Nonnull
        public ThirExpr getRight() {
            return right;
        }
    }

    public static class Logical extends ThirExpr {
        @Nonnull public final LogicOp op;
        @Nonnull public final ThirExpr left;
        @Nonnull public final ThirExpr right;
        public Logical(@Nonnull LogicOp op, @Nonnull ThirExpr left, @Nonnull ThirExpr right, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Nonnull
        public LogicOp getOp() {
            return op;
        }

        @Nonnull
        public ThirExpr getLeft() {
            return left;
        }

        @Nonnull
        public ThirExpr getRight() {
            return right;
        }
    }

    public static class Block extends ThirExpr {
        @Nonnull public final ThirBlock block;
        public Block(@Nonnull ThirBlock block, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.block = block;
        }

        @Nonnull
        public ThirBlock getBlock() {
            return block;
        }
    }

    public static class If extends ThirExpr {
        @Nonnull public final Scope.IfThen ifThenScope;
        @Nonnull public final ThirExpr cond;
        @Nonnull public final ThirExpr then;
        @Nullable public final ThirExpr elseExpr;
        public If(@Nonnull Scope.IfThen ifThenScope, @Nonnull ThirExpr cond, @Nonnull ThirExpr then, @Nullable ThirExpr elseExpr, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.ifThenScope = ifThenScope;
            this.cond = cond;
            this.then = then;
            this.elseExpr = elseExpr;
        }

        @Nonnull
        public Scope.IfThen getIfThenScope() {
            return ifThenScope;
        }

        @Nonnull
        public ThirExpr getCond() {
            return cond;
        }

        @Nonnull
        public ThirExpr getThen() {
            return then;
        }

        @Nullable
        public ThirExpr getElseExpr() {
            return elseExpr;
        }
    }

    public static class Array extends ThirExpr {
        @Nonnull public final List<ThirExpr> fields;
        public Array(@Nonnull List<ThirExpr> fields, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.fields = fields;
        }

        @Nonnull
        public List<ThirExpr> getFields() {
            return fields;
        }
    }

    public static class Repeat extends ThirExpr {
        @Nonnull public final ThirExpr value;
        @Nonnull public final Const count;
        public Repeat(@Nonnull ThirExpr value, @Nonnull Const count, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.value = value;
            this.count = count;
        }

        @Nonnull
        public ThirExpr getValue() {
            return value;
        }

        @Nonnull
        public Const getCount() {
            return count;
        }
    }

    public static class Tuple extends ThirExpr {
        @Nonnull public final List<ThirExpr> fields;
        public Tuple(@Nonnull List<ThirExpr> fields, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.fields = fields;
        }

        @Nonnull
        public List<ThirExpr> getFields() {
            return fields;
        }
    }

    public static class Field extends ThirExpr {
        @Nonnull public final ThirExpr expr;
        public final int fieldIndex;
        public Field(@Nonnull ThirExpr expr, int fieldIndex, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.expr = expr;
            this.fieldIndex = fieldIndex;
        }

        @Nonnull
        public ThirExpr getExpr() {
            return expr;
        }

        public int getFieldIndex() {
            return fieldIndex;
        }
    }

    public static class Loop extends ThirExpr {
        @Nonnull public final ThirExpr body;
        public Loop(@Nonnull ThirExpr body, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.body = body;
        }

        @Nonnull
        public ThirExpr getBody() {
            return body;
        }
    }

    public static class NeverToAny extends ThirExpr {
        @Nonnull public final ThirExpr spanExpr;
        public NeverToAny(@Nonnull ThirExpr spanExpr, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.spanExpr = spanExpr;
        }

        @Nonnull
        public ThirExpr getSpanExpr() {
            return spanExpr;
        }
    }

    public static class Break extends ThirExpr {
        @Nonnull public final Scope label;
        @Nullable public final ThirExpr expr;
        public Break(@Nonnull Scope label, @Nullable ThirExpr expr, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.label = label;
            this.expr = expr;
        }

        @Nonnull
        public Scope getLabel() {
            return label;
        }

        @Nullable
        public ThirExpr getExpr() {
            return expr;
        }
    }

    public static class Continue extends ThirExpr {
        public Continue(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class Return extends ThirExpr {
        public Return(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class VarRef extends ThirExpr {
        @Nonnull public final LocalVar local;
        public VarRef(@Nonnull LocalVar local, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.local = local;
        }

        @Nonnull
        public LocalVar getLocal() {
            return local;
        }
    }

    public static class UpvarRef extends ThirExpr {
        public UpvarRef(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class Assign extends ThirExpr {
        @Nonnull public final ThirExpr left;
        @Nonnull public final ThirExpr right;
        public Assign(@Nonnull ThirExpr left, @Nonnull ThirExpr right, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.left = left;
            this.right = right;
        }

        @Nonnull
        public ThirExpr getLeft() {
            return left;
        }

        @Nonnull
        public ThirExpr getRight() {
            return right;
        }
    }

    public static class AssignOp extends ThirExpr {
        @Nonnull public final BinaryOperator op;
        @Nonnull public final ThirExpr left;
        @Nonnull public final ThirExpr right;
        public AssignOp(@Nonnull BinaryOperator op, @Nonnull ThirExpr left, @Nonnull ThirExpr right, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Nonnull
        public BinaryOperator getOp() {
            return op;
        }

        @Nonnull
        public ThirExpr getLeft() {
            return left;
        }

        @Nonnull
        public ThirExpr getRight() {
            return right;
        }
    }

    public static class Adt extends ThirExpr {
        @Nonnull public final RsStructOrEnumItemElement definition;
        public final int variantIndex;
        @Nonnull public final List<FieldExpr> fields;
        @Nullable public final FruInfo base;
        public Adt(@Nonnull RsStructOrEnumItemElement definition, int variantIndex, @Nonnull List<FieldExpr> fields, @Nullable FruInfo base, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.definition = definition;
            this.variantIndex = variantIndex;
            this.fields = fields;
            this.base = base;
        }

        @Nonnull
        public RsStructOrEnumItemElement getDefinition() {
            return definition;
        }

        public int getVariantIndex() {
            return variantIndex;
        }

        @Nonnull
        public List<FieldExpr> getFields() {
            return fields;
        }

        @Nullable
        public FruInfo getBase() {
            return base;
        }
    }

    public static class Borrow extends ThirExpr {
        @Nonnull public final MirBorrowKind kind;
        @Nonnull public final ThirExpr arg;
        public Borrow(@Nonnull MirBorrowKind kind, @Nonnull ThirExpr arg, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.kind = kind;
            this.arg = arg;
        }

        @Nonnull
        public MirBorrowKind getKind() {
            return kind;
        }

        @Nonnull
        public ThirExpr getArg() {
            return arg;
        }
    }

    public static class AddressOf extends ThirExpr {
        public AddressOf(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class BoxExpr extends ThirExpr {
        public BoxExpr(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class Call extends ThirExpr {
        @Nonnull public final Ty fnTy;
        @Nonnull public final ThirExpr callee;
        @Nonnull public final List<ThirExpr> args;
        public final boolean fromCall;
        public Call(@Nonnull Ty fnTy, @Nonnull ThirExpr callee, @Nonnull List<ThirExpr> args, boolean fromCall, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.fnTy = fnTy;
            this.callee = callee;
            this.args = args;
            this.fromCall = fromCall;
        }

        @Nonnull
        public Ty getFnTy() {
            return fnTy;
        }

        @Nonnull
        public ThirExpr getCallee() {
            return callee;
        }

        @Nonnull
        public List<ThirExpr> getArgs() {
            return args;
        }

        public boolean getFromCall() {
            return fromCall;
        }
    }

    public static class Deref extends ThirExpr {
        @Nonnull public final ThirExpr arg;
        public Deref(@Nonnull ThirExpr arg, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.arg = arg;
        }

        @Nonnull
        public ThirExpr getArg() {
            return arg;
        }
    }

    public static class Cast extends ThirExpr {
        @Nonnull public final ThirExpr source;
        public Cast(@Nonnull ThirExpr source, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.source = source;
        }

        @Nonnull
        public ThirExpr getSource() {
            return source;
        }
    }

    public static class Use extends ThirExpr {
        @Nonnull public final ThirExpr source;
        public Use(@Nonnull ThirExpr source, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.source = source;
        }

        @Nonnull
        public ThirExpr getSource() {
            return source;
        }
    }

    public static class Pointer extends ThirExpr {
        public Pointer(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class Let extends ThirExpr {
        @Nonnull public final ThirPat pat;
        @Nonnull public final ThirExpr expr;
        public Let(@Nonnull ThirPat pat, @Nonnull ThirExpr expr, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.pat = pat;
            this.expr = expr;
        }

        @Nonnull
        public ThirPat getPat() {
            return pat;
        }

        @Nonnull
        public ThirExpr getExpr() {
            return expr;
        }
    }

    public static class Match extends ThirExpr {
        @Nonnull public final ThirExpr expr;
        @Nonnull public final List<MirMatch.MirArm> arms;
        public Match(@Nonnull ThirExpr expr, @Nonnull List<MirMatch.MirArm> arms, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.expr = expr;
            this.arms = arms;
        }

        @Nonnull
        public ThirExpr getExpr() {
            return expr;
        }

        @Nonnull
        public List<MirMatch.MirArm> getArms() {
            return arms;
        }
    }

    public static class Index extends ThirExpr {
        @Nonnull public final ThirExpr lhs;
        @Nonnull public final ThirExpr index;
        public Index(@Nonnull ThirExpr lhs, @Nonnull ThirExpr index, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(ty, span);
            this.lhs = lhs;
            this.index = index;
        }

        @Nonnull
        public ThirExpr getLhs() {
            return lhs;
        }

        @Nonnull
        public ThirExpr getIndex() {
            return index;
        }
    }

    public static class ConstBlock extends ThirExpr {
        public ConstBlock(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class PlaceTypeAscription extends ThirExpr {
        public PlaceTypeAscription(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class ValueTypeAscription extends ThirExpr {
        public ValueTypeAscription(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class Closure extends ThirExpr {
        public Closure(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class InlineAsm extends ThirExpr {
        public InlineAsm(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class OffsetOf extends ThirExpr {
        public OffsetOf(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class ThreadLocalRef extends ThirExpr {
        public ThreadLocalRef(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }

    public static class Yield extends ThirExpr {
        public Yield(@Nonnull Ty ty, @Nonnull MirSpan span) { super(ty, span); }
    }
}
