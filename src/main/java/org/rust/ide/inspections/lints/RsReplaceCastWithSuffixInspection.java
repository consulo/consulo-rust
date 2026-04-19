/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.ReplaceCastWithLiteralSuffixFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyFloat;
import org.rust.lang.core.types.ty.TyInteger;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.RsLiteralKindUtil;

public class RsReplaceCastWithSuffixInspection extends RsLintInspection {

    private static final List<String> RADIX_PREFIXES = Arrays.asList("0x", "0b", "0o");

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.UnnecessaryCast;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitCastExpr(@NotNull RsCastExpr castExpr) {
                RsExpr expr = castExpr.getExpr();
                RsTypeReference typeReference = castExpr.getTypeReference();
                Ty typeReferenceType = RsTypesUtil.getRawType(typeReference);
                if (!(typeReferenceType instanceof TyInteger) && !(typeReferenceType instanceof TyFloat)) {
                    return;
                }
                if (typeReferenceType.getAliasedBy() != null) {
                    return;
                }

                if (!isValidSuffix(expr, typeReference.getText())) {
                    return;
                }

                registerLintProblem(
                    holder,
                    castExpr,
                    RsBundle.message("inspection.message.can.be.replaced.with.literal.suffix"),
                    RsLintHighlightingType.WEAK_WARNING,
                    Collections.singletonList(new ReplaceCastWithLiteralSuffixFix(castExpr))
                );
            }
        };
    }

    private boolean isValidSuffix(@NotNull RsExpr expr, @NotNull String suffix) {
        RsLitExpr litExpr;
        if (expr instanceof RsLitExpr) {
            litExpr = (RsLitExpr) expr;
        } else if (expr instanceof RsUnaryExpr) {
            RsUnaryExpr unaryExpr = (RsUnaryExpr) expr;
            if (unaryExpr.getMinus() == null) return false;
            RsExpr innerExpr = unaryExpr.getExpr();
            if (innerExpr instanceof RsLitExpr) {
                litExpr = (RsLitExpr) innerExpr;
            } else {
                return false;
            }
        } else {
            return false;
        }

        RsLiteralKind kind = RsLiteralKindUtil.getKind(litExpr);
        if (!(kind instanceof RsLiteralKind.RsLiteralWithSuffix)) return false;

        RsLiteralKind.RsLiteralWithSuffix withSuffix = (RsLiteralKind.RsLiteralWithSuffix) kind;
        return withSuffix.getSuffix() == null && isValidSuffixForKind(withSuffix, suffix);
    }

    private boolean isValidSuffixForKind(@NotNull RsLiteralKind.RsLiteralWithSuffix kind, @NotNull String suffix) {
        // Special case for `1f32`, which is allowed even though f32 is not a valid integer suffix
        if (kind instanceof RsLiteralKind.IntegerLiteral && TyFloat.NAMES.contains(suffix)
            // But `0b11f32` is not allowed
            && !startsWithRadixPrefix((RsLiteralKind.IntegerLiteral) kind)) {
            return true;
        }
        return kind.getValidSuffixes().contains(suffix);
    }

    private boolean startsWithRadixPrefix(@NotNull RsLiteralKind.IntegerLiteral kind) {
        String text = kind.getNode().getText();
        for (String prefix : RADIX_PREFIXES) {
            if (text.startsWith(prefix)) return true;
        }
        return false;
    }
}
