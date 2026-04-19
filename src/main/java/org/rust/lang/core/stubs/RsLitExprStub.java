/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.ext.RsLitExprUtil;
import org.rust.lang.core.psi.impl.RsLitExprImpl;
import org.rust.lang.core.types.ty.TyFloat;
import org.rust.lang.core.types.ty.TyInteger;

import java.io.IOException;

public final class RsLitExprStub extends RsPlaceholderStub<RsLitExpr> {
    @Nullable
    private final RsStubLiteralKind kind;

    public RsLitExprStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, @Nullable RsStubLiteralKind kind) {
        super(parent, elementType);
        this.kind = kind;
    }

    @Nullable
    public RsStubLiteralKind getKind() {
        return kind;
    }

    public static final RsStubElementType<RsLitExprStub, RsLitExpr> Type =
        new RsStubElementType<RsLitExprStub, RsLitExpr>("LIT_EXPR") {
            @Override
            public boolean shouldCreateStub(@NotNull ASTNode node) {
                return StubImplementationsKt.shouldCreateExprStub(node);
            }

            @Override
            public void serialize(@NotNull RsLitExprStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                RsStubLiteralKind kind = stub.getKind();
                serializeKind(dataStream, kind);
            }

            @NotNull
            @Override
            public RsLitExprStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                RsStubLiteralKind kind = deserializeKind(dataStream);
                return new RsLitExprStub(parentStub, this, kind);
            }

            @NotNull
            @Override
            public RsLitExpr createPsi(@NotNull RsLitExprStub stub) {
                return new RsLitExprImpl(stub, this);
            }

            @NotNull
            @Override
            public RsLitExprStub createStub(@NotNull RsLitExpr psi, @Nullable StubElement<?> parentStub) {
                RsStubLiteralKind kind = RsLitExprUtil.getStubKind(psi);
                return new RsLitExprStub(parentStub, this, kind);
            }
        };

    private static void serializeKind(@NotNull StubOutputStream ds, @Nullable RsStubLiteralKind kind) throws IOException {
        if (kind instanceof RsStubLiteralKind.Boolean) {
            ds.writeByte(0);
            ds.writeBoolean(((RsStubLiteralKind.Boolean) kind).getValue());
        } else if (kind instanceof RsStubLiteralKind.Integer) {
            ds.writeByte(1);
            RsStubLiteralKind.Integer intKind = (RsStubLiteralKind.Integer) kind;
            StubImplementationsKt.writeLongAsNullable(ds, intKind.getValue());
            StubImplementationsKt.writeUTFFastAsNullable(ds, intKind.getSuffix());
        } else if (kind instanceof RsStubLiteralKind.Float) {
            ds.writeByte(2);
            RsStubLiteralKind.Float floatKind = (RsStubLiteralKind.Float) kind;
            StubImplementationsKt.writeDoubleAsNullable(ds, floatKind.getValue());
            StubImplementationsKt.writeUTFFastAsNullable(ds, floatKind.getSuffix());
        } else if (kind instanceof RsStubLiteralKind.Char) {
            ds.writeByte(3);
            RsStubLiteralKind.Char charKind = (RsStubLiteralKind.Char) kind;
            StubImplementationsKt.writeUTFFastAsNullable(ds, charKind.getValue());
            ds.writeBoolean(charKind.isByte());
        } else if (kind instanceof RsStubLiteralKind.StringLiteral) {
            ds.writeByte(4);
            RsStubLiteralKind.StringLiteral strKind = (RsStubLiteralKind.StringLiteral) kind;
            StubImplementationsKt.writeUTFFastAsNullable(ds, strKind.getValue());
            ds.writeBoolean(strKind.isByte());
            ds.writeBoolean(strKind.isCStr());
        } else {
            ds.writeByte(-1);
        }
    }

    @Nullable
    private static RsStubLiteralKind deserializeKind(@NotNull StubInputStream ds) throws IOException {
        byte tag = ds.readByte();
        switch (tag) {
            case 0:
                return new RsStubLiteralKind.Boolean(ds.readBoolean());
            case 1: {
                Long value = StubImplementationsKt.readLongAsNullable(ds);
                java.lang.String suffix = StubImplementationsKt.readUTFFastAsNullable(ds);
                return new RsStubLiteralKind.Integer(value, suffix);
            }
            case 2: {
                Double value = StubImplementationsKt.readDoubleAsNullable(ds);
                java.lang.String suffix = StubImplementationsKt.readUTFFastAsNullable(ds);
                return new RsStubLiteralKind.Float(value, suffix);
            }
            case 3: {
                java.lang.String value = StubImplementationsKt.readUTFFastAsNullable(ds);
                boolean isByte = ds.readBoolean();
                return new RsStubLiteralKind.Char(value, isByte);
            }
            case 4: {
                java.lang.String value = StubImplementationsKt.readUTFFastAsNullable(ds);
                boolean isByte = ds.readBoolean();
                boolean isCStr = ds.readBoolean();
                return new RsStubLiteralKind.StringLiteral(value, isByte, isCStr);
            }
            default:
                return null;
        }
    }
}
