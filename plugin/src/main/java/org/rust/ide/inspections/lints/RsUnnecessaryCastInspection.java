/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.fixes.RemoveCastFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.Collections;
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.impl.*;

@ExtensionImpl
public class RsUnnecessaryCastInspection extends RsLintInspection {

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.UnnecessaryCast;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitCastExpr(@Nonnull RsCastExpr castExpr) {
                Ty typeReferenceType = RsTypesUtil.getRawType(castExpr.getTypeReference());
                Ty exprType = RsTypesUtil.getType(castExpr.getExpr());

                if (isAlias(typeReferenceType)) return;
                if (isAlias(exprType)) return;

                if (containsUnknown(typeReferenceType)) return;
                if (containsUnknown(exprType)) return;

                if (isUnsuffixedNumber(castExpr.getExpr())) return;

                if (exprType.isEquivalentTo(typeReferenceType)) {
                    PsiElement castAs = castExpr.getAs();
                    if (castAs == null) return;
                    int start = castAs.getTextRange().getStartOffset() - castExpr.getTextRange().getStartOffset();
                    int end = castExpr.getTypeReference().getTextRange().getEndOffset() - castExpr.getTextRange().getStartOffset();
                    registerLintProblem(
                        holder,
                        castExpr,
                        RsBundle.message("inspection.message.unnecessary.cast"),
                        new TextRange(start, end),
                        RsLintHighlightingType.UNUSED_SYMBOL,
                        Collections.singletonList(new RemoveCastFix(castExpr))
                    );
                }
            }
        };
    }

    private boolean isAlias(@Nonnull Ty ty) {
        TypeVisitor visitor = new TypeVisitor() {
            @Override
            public boolean visitTy(@Nonnull Ty t) {
                return t.getAliasedBy() != null || t.superVisitWith(this);
            }
        };
        return ty.visitWith(visitor);
    }

    private boolean isUnsuffixedNumber(@Nonnull RsExpr expr) {
        if (!(expr instanceof RsLitExpr)) return false;
        RsLiteralKind kind = RsLiteralKindUtil.getKind((RsLitExpr) expr);
        if (!(kind instanceof RsLiteralKind.RsLiteralWithSuffix)) return false;
        return ((RsLiteralKind.RsLiteralWithSuffix) kind).getSuffix() == null;
    }

    private boolean containsUnknown(@Nonnull Ty ty) {
        return FoldUtil.containsTyOfClass(ty, TyUnknown.class);
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.unnecessary.cast.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("lints"));
    }

    @Nonnull
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.of(RsBundle.message("rust"))};
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WEAK_WARNING;
    }
}
