/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.RsLiteralKind;
import org.rust.lang.core.psi.RsLiteralKindUtil;
import org.rust.lang.core.stubs.RsLitExprStub;
import org.rust.lang.core.stubs.RsStubLiteralKind;

public final class RsLitExprUtil {
    private RsLitExprUtil() {
    }

    @Nullable
    public static RsStubLiteralKind getStubKind(@NotNull RsLitExpr litExpr) {
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
    public static java.lang.Boolean getBooleanValue(@NotNull RsLitExpr litExpr) {
        RsStubLiteralKind kind = getStubKind(litExpr);
        if (kind instanceof RsStubLiteralKind.Boolean) {
            return ((RsStubLiteralKind.Boolean) kind).getValue();
        }
        return null;
    }

    @Nullable
    public static Long getIntegerValue(@NotNull RsLitExpr litExpr) {
        RsStubLiteralKind kind = getStubKind(litExpr);
        if (kind instanceof RsStubLiteralKind.Integer) {
            return ((RsStubLiteralKind.Integer) kind).getValue();
        }
        return null;
    }

    @Nullable
    public static Double getFloatValue(@NotNull RsLitExpr litExpr) {
        RsStubLiteralKind kind = getStubKind(litExpr);
        if (kind instanceof RsStubLiteralKind.Float) {
            return ((RsStubLiteralKind.Float) kind).getValue();
        }
        return null;
    }

    @Nullable
    public static java.lang.String getCharValue(@NotNull RsLitExpr litExpr) {
        RsStubLiteralKind kind = getStubKind(litExpr);
        if (kind instanceof RsStubLiteralKind.Char) {
            return ((RsStubLiteralKind.Char) kind).getValue();
        }
        return null;
    }

    @Nullable
    public static java.lang.String getStringValue(@NotNull RsLitExpr litExpr) {
        RsStubLiteralKind kind = getStubKind(litExpr);
        if (kind instanceof RsStubLiteralKind.StringLiteral) {
            return ((RsStubLiteralKind.StringLiteral) kind).getValue();
        }
        return null;
    }

    public static boolean containsOffset(@NotNull RsLitExpr litExpr, int offset) {
        TextRange range = ElementManipulators.getValueTextRange(litExpr).shiftRight(litExpr.getTextOffset());
        return range.containsOffset(offset);
    }
}
