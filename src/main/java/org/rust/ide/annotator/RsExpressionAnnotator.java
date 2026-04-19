/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.AddStructFieldsFix;
import org.rust.ide.fixes.CreateStructFieldFromConstructorFix;
import org.rust.ide.fixes.RemoveRedundantParenthesesFix;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ref.RsReferenceExtUtil;

import java.util.*;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;

public class RsExpressionAnnotator extends AnnotatorBase {
    @Override
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        RsAnnotationHolder rsHolder = new RsAnnotationHolder(holder);
        element.accept(new RedundantParenthesisVisitor(rsHolder));
        if (element instanceof RsStructLiteral) {
            RsStructLiteral structLiteral = (RsStructLiteral) element;
            PsiElement resolved = structLiteral.getPath().getReference() != null
                ? RsPathReferenceImpl.deepResolve(structLiteral.getPath().getReference())
                : null;
            if (resolved instanceof RsFieldsOwner) {
                checkStructLiteral(rsHolder, (RsFieldsOwner) resolved, structLiteral);
            }
        }
    }

    private void checkStructLiteral(
        @NotNull RsAnnotationHolder holder,
        @NotNull RsFieldsOwner decl,
        @NotNull RsStructLiteral literal
    ) {
        RsStructLiteralBody body = literal.getStructLiteralBody();
        for (RsStructLiteralField field : body.getStructLiteralFieldList()) {
            boolean hasFieldDecl = false;
            for (Object resolved : field.getReference().multiResolve()) {
                if (resolved instanceof RsFieldDecl) {
                    hasFieldDecl = true;
                    break;
                }
            }
            if (!hasFieldDecl) {
                AnnotationBuilder annotationBuilder = holder.newErrorAnnotation(field.getReferenceNameElement(), RsBundle.message("inspection.message.no.such.field"));
                if (annotationBuilder == null) continue;

                CreateStructFieldFromConstructorFix fix = CreateStructFieldFromConstructorFix.tryCreate(field);
                if (fix != null) {
                    annotationBuilder.withFix(fix);
                }

                annotationBuilder.highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL).create();
            }
        }

        for (RsStructLiteralField field : findDuplicateReferences(body.getStructLiteralFieldList())) {
            holder.createErrorAnnotation(field.getReferenceNameElement(), RsBundle.message("inspection.message.duplicate.field"));
        }

        if (body.getDotdot() != null) return;

        if (decl instanceof RsStructItem && RsStructItemUtil.getKind((RsStructItem) decl) == RsStructKind.UNION) return;

        if (!calculateMissingFields(body, decl).isEmpty()) {
            if (!RsElementUtil.existsAfterExpansion(literal)) return;

            RsPath path = RsPsiJavaUtil.descendantOfTypeStrict(literal, RsPath.class);
            TextRange structNameRange = path != null ? path.getTextRange() : null;
            if (structNameRange != null) {
                holder.getHolder().newAnnotation(HighlightSeverity.ERROR, RsBundle.message("inspection.message.some.fields.are.missing"))
                    .range(structNameRange)
                    .newFix(new AddStructFieldsFix(literal, false)).range(body.getParent().getTextRange()).registerFix()
                    .newFix(new AddStructFieldsFix(literal, true)).range(body.getParent().getTextRange()).registerFix()
                    .create();
            }
        }
    }

    @NotNull
    public static List<RsFieldDecl> calculateMissingFields(@NotNull RsStructLiteralBody expr, @NotNull RsFieldsOwner decl) {
        Set<String> declaredFields = new HashSet<>();
        for (RsStructLiteralField field : expr.getStructLiteralFieldList()) {
            declaredFields.add(field.getReferenceName());
        }
        List<RsFieldDecl> result = new ArrayList<>();
        for (RsFieldDecl field : decl.getFields()) {
            if (field.getName() != null && !declaredFields.contains(field.getName())) {
                result.add(field);
            }
        }
        return result;
    }

    @NotNull
    private static <T extends RsMandatoryReferenceElement> Collection<T> findDuplicateReferences(@NotNull Collection<T> items) {
        Set<String> names = new HashSet<>(items.size());
        SmartList<T> result = new SmartList<>();
        for (T item : items) {
            String name = item.getReferenceName();
            if (names.contains(name)) {
                result.add(item);
            }
            names.add(name);
        }
        return result;
    }

    private static class RedundantParenthesisVisitor extends RsVisitor {
        private final RsAnnotationHolder myHolder;

        RedundantParenthesisVisitor(@NotNull RsAnnotationHolder holder) {
            this.myHolder = holder;
        }

        @Override
        public void visitCondition(@NotNull RsCondition o) {
            warnIfParens(o.getExpr(), RsBundle.message("inspection.message.predicate.expression.has.unnecessary.parentheses"));
        }

        @Override
        public void visitRetExpr(@NotNull RsRetExpr o) {
            warnIfParens(o.getExpr(), RsBundle.message("inspection.message.return.expression.has.unnecessary.parentheses"));
        }

        @Override
        public void visitMatchExpr(@NotNull RsMatchExpr o) {
            warnIfParens(o.getExpr(), RsBundle.message("inspection.message.match.expression.has.unnecessary.parentheses"));
        }

        @Override
        public void visitForExpr(@NotNull RsForExpr o) {
            warnIfParens(o.getExpr(), RsBundle.message("inspection.message.for.loop.expression.has.unnecessary.parentheses"));
        }

        @Override
        public void visitParenExpr(@NotNull RsParenExpr o) {
            if (!(o.getParent() instanceof RsParenExpr)) {
                warnIfParens(o.getExpr(), RsBundle.message("inspection.message.redundant.parentheses.in.expression"));
            }
        }

        private void warnIfParens(RsExpr expr, String message) {
            if (!(expr instanceof RsParenExpr)) return;
            if (!canWarn((RsParenExpr) expr)) return;
            myHolder.createWeakWarningAnnotation(expr, message, new RemoveRedundantParenthesesFix((RsParenExpr) expr));
        }

        private boolean canWarn(@NotNull RsParenExpr expr) {
            if (PsiTreeUtil.getContextOfType(
                expr,
                false,
                RsCondition.class,
                RsMatchExpr.class,
                RsForExpr.class
            ) == null) return true;

            PsiElement[] children = expr.getChildren();
            if (children.length != 1) return true;
            PsiElement child = children[0];

            if (child instanceof RsStructLiteral) return false;
            if (child instanceof RsBinaryExpr) {
                for (RsExpr e : ((RsBinaryExpr) child).getExprList()) {
                    if (e instanceof RsStructLiteral) return false;
                }
                return true;
            }
            return true;
        }
    }
}
