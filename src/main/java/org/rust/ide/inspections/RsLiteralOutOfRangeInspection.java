/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.fixes.ConvertTypeReferenceFix;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyInteger;
import org.rust.lang.utils.RsDiagnostic;

import java.util.List;
import org.rust.lang.core.psi.RsLiteralKindUtil;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;

public class RsLiteralOutOfRangeInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitLitExpr(@NotNull RsLitExpr expr) {
                Object expectedTy = RsTypesUtil.getType(expr);
                if (!(expectedTy instanceof TyInteger)) return;
                TyInteger tyInt = (TyInteger) expectedTy;
                if (tyInt instanceof TyInteger.U128
                    || tyInt instanceof TyInteger.I128
                    || tyInt instanceof TyInteger.I32
                    || tyInt instanceof TyInteger.U64
                    || tyInt instanceof TyInteger.I64
                    || tyInt instanceof TyInteger.USize) return;

                RsLiteralKind kind = RsLiteralKindUtil.getKind(expr);
                if (!(kind instanceof RsLiteralKind.IntegerLiteral)) return;
                RsLiteralKind.IntegerLiteral literal = (RsLiteralKind.IntegerLiteral) kind;

                boolean isNegative = false;
                if (expr.getContext() instanceof RsUnaryExpr) {
                    isNegative = ((RsUnaryExpr) expr.getContext()).getMinus() != null;
                }
                Long value = literal.getValue();
                if (value == null) return;
                long numericValue = isNegative ? -value : value;
                long[] validRange = ConstExprEvaluator.getValidValuesRange(tyInt);
                if (numericValue < validRange[0] || numericValue > validRange[1]) {
                    LocalQuickFix fix = findQuickFix(expr, tyInt, numericValue);
                    new RsDiagnostic.LiteralOutOfRange(expr, String.valueOf(numericValue), TypeRendering.render(tyInt), fix)
                        .addToHolder(holder);
                }
            }

            @Nullable
            private LocalQuickFix findQuickFix(@NotNull RsLitExpr expr, @NotNull TyInteger currentType, long overflownValue) {
                TyInteger proposedTy = findTypeUpgrade(currentType, overflownValue);
                if (proposedTy == null) return null;
                Object parent = expr.getContext();
                if (parent instanceof RsLetDecl) {
                    RsLetDecl letDecl = (RsLetDecl) parent;
                    RsPat pat = letDecl.getPat();
                    if (!(pat instanceof RsPatIdent)) return null;
                    RsPatIdent patIdent = (RsPatIdent) pat;
                    RsTypeReference typeReference = letDecl.getTypeReference();
                    if (typeReference == null) return null;
                    return new ConvertTypeReferenceFix(typeReference, patIdent.getPatBinding().getIdentifier().getText(), proposedTy);
                } else if (parent instanceof RsStructLiteralField) {
                    RsStructLiteralField field = (RsStructLiteralField) parent;
                    Object resolved = field.getReference().resolve();
                    if (!(resolved instanceof RsNamedFieldDecl)) return null;
                    RsNamedFieldDecl fieldDecl = (RsNamedFieldDecl) resolved;
                    RsTypeReference typeReference = fieldDecl.getTypeReference();
                    if (typeReference == null) return null;
                    if (fieldDecl.getContainingCrate().getOrigin() != PackageOrigin.WORKSPACE) return null;
                    return new ConvertTypeReferenceFix(typeReference, fieldDecl.getIdentifier().getText(), proposedTy);
                }
                return null;
            }

            @Nullable
            private TyInteger findTypeUpgrade(@NotNull TyInteger currentType, long overflownValue) {
                List<TyInteger> values = java.util.Arrays.asList(
                    TyInteger.I8.INSTANCE, TyInteger.I16.INSTANCE, TyInteger.I32.INSTANCE,
                    TyInteger.I64.INSTANCE, TyInteger.I128.INSTANCE, TyInteger.ISize.INSTANCE,
                    TyInteger.U8.INSTANCE, TyInteger.U16.INSTANCE, TyInteger.U32.INSTANCE,
                    TyInteger.U64.INSTANCE, TyInteger.U128.INSTANCE, TyInteger.USize.INSTANCE
                );
                int i = values.indexOf(currentType);
                if (i < 0) return null;
                for (int j = i; j < values.size(); j++) {
                    TyInteger ty = values.get(j);
                    long[] range = ConstExprEvaluator.getValidValuesRange(ty);
                    if (overflownValue >= range[0] && overflownValue <= range[1]) {
                        return ty;
                    }
                }
                return null;
            }
        };
    }
}
