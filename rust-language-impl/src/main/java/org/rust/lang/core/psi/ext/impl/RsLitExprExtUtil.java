/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.document.util.TextRange;
import consulo.language.psi.ElementManipulators;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.impl.RsLiteralKind;
import org.rust.lang.core.psi.impl.RsLiteralKindUtil;
import org.rust.lang.core.stubs.RsLitExprStub;
import org.rust.lang.core.stubs.RsStubLiteralKind;
import org.rust.lang.core.psi.ext.*;

public final class RsLitExprExtUtil {
    private RsLitExprExtUtil() {
    }

    @Nullable
    public static RsStubLiteralKind getStubKind(@Nonnull RsLitExpr litExpr) {
        Object stub = RsPsiJavaUtil.getGreenStub(litExpr);
        if (stub instanceof RsLitExprStub) {
            return ((RsLitExprStub) stub).getKind();
        }
        RsLiteralKind kind = RsLiteralKindUtil.getKind(litExpr);
        if (kind instanceof RsLiteralKind.BooleanLiteral) {
            return new RsStubLiteralKind.Boolean(((RsLiteralKind.BooleanLiteral) kind).getValue());
        }
        if (kind instanceof RsLiteralKind.CharLiteral) {
            return new RsStubLiteralKind.Char(
                ((RsLiteralKind.CharLiteral) kind).getValue(),
                ((RsLiteralKind.CharLiteral) kind).isByte()
            );
        }
        if (kind instanceof RsLiteralKind.StringLiteral) {
            return new RsStubLiteralKind.StringLiteral(
                ((RsLiteralKind.StringLiteral) kind).getValue(),
                ((RsLiteralKind.StringLiteral) kind).isByte(),
                ((RsLiteralKind.StringLiteral) kind).isCStr()
            );
        }
        if (kind instanceof RsLiteralKind.IntegerLiteral) {
            return new RsStubLiteralKind.Integer(
                ((RsLiteralKind.IntegerLiteral) kind).getValue(),
                ((RsLiteralKind.IntegerLiteral) kind).getSuffix()
            );
        }
        if (kind instanceof RsLiteralKind.FloatLiteral) {
            return new RsStubLiteralKind.Float(
                ((RsLiteralKind.FloatLiteral) kind).getValue(),
                ((RsLiteralKind.FloatLiteral) kind).getSuffix()
            );
        }
        return null;
    }

    @Nullable
    public static java.lang.Boolean getBooleanValue(@Nonnull RsLitExpr litExpr) {
        RsStubLiteralKind kind = getStubKind(litExpr);
        if (kind instanceof RsStubLiteralKind.Boolean) {
            return ((RsStubLiteralKind.Boolean) kind).getValue();
        }
        return null;
    }

    @Nullable
    public static Long getIntegerValue(@Nonnull RsLitExpr litExpr) {
        RsStubLiteralKind kind = getStubKind(litExpr);
        if (kind instanceof RsStubLiteralKind.Integer) {
            return ((RsStubLiteralKind.Integer) kind).getValue();
        }
        return null;
    }

    @Nullable
    public static Double getFloatValue(@Nonnull RsLitExpr litExpr) {
        RsStubLiteralKind kind = getStubKind(litExpr);
        if (kind instanceof RsStubLiteralKind.Float) {
            return ((RsStubLiteralKind.Float) kind).getValue();
        }
        return null;
    }

    @Nullable
    public static java.lang.String getCharValue(@Nonnull RsLitExpr litExpr) {
        RsStubLiteralKind kind = getStubKind(litExpr);
        if (kind instanceof RsStubLiteralKind.Char) {
            return ((RsStubLiteralKind.Char) kind).getValue();
        }
        return null;
    }

    @Nullable
    public static java.lang.String getStringValue(@Nonnull RsLitExpr litExpr) {
        RsStubLiteralKind kind = getStubKind(litExpr);
        if (kind instanceof RsStubLiteralKind.StringLiteral) {
            return ((RsStubLiteralKind.StringLiteral) kind).getValue();
        }
        return null;
    }

    public static boolean containsOffset(@Nonnull RsLitExpr litExpr, int offset) {
        TextRange range = ElementManipulators.getValueTextRange(litExpr).shiftRight(litExpr.getTextOffset());
        return range.containsOffset(offset);
    }
}
