/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.annotation.AnnotationBuilder;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.fixes.AddStructFieldsFix;
import org.rust.ide.fixes.CreateStructFieldFromConstructorFix;
import org.rust.ide.fixes.RemoveRedundantParenthesesFix;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.*;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;
import org.rust.lang.core.psi.ext.impl.*;

public class RsExpressionAnnotator extends AnnotatorBase {
    @Override
    protected void annotateInternal(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
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
        @Nonnull RsAnnotationHolder holder,
        @Nonnull RsFieldsOwner decl,
        @Nonnull RsStructLiteral literal
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

    @Nonnull
    public static List<RsFieldDecl> calculateMissingFields(@Nonnull RsStructLiteralBody expr, @Nonnull RsFieldsOwner decl) {
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

    @Nonnull
    private static <T extends RsMandatoryReferenceElement> Collection<T> findDuplicateReferences(@Nonnull Collection<T> items) {
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

        RedundantParenthesisVisitor(@Nonnull RsAnnotationHolder holder) {
            this.myHolder = holder;
        }

        @Override
        public void visitCondition(@Nonnull RsCondition o) {
            warnIfParens(o.getExpr(), RsBundle.message("inspection.message.predicate.expression.has.unnecessary.parentheses"));
        }

        @Override
        public void visitRetExpr(@Nonnull RsRetExpr o) {
            warnIfParens(o.getExpr(), RsBundle.message("inspection.message.return.expression.has.unnecessary.parentheses"));
        }

        @Override
        public void visitMatchExpr(@Nonnull RsMatchExpr o) {
            warnIfParens(o.getExpr(), RsBundle.message("inspection.message.match.expression.has.unnecessary.parentheses"));
        }

        @Override
        public void visitForExpr(@Nonnull RsForExpr o) {
            warnIfParens(o.getExpr(), RsBundle.message("inspection.message.for.loop.expression.has.unnecessary.parentheses"));
        }

        @Override
        public void visitParenExpr(@Nonnull RsParenExpr o) {
            if (!(o.getParent() instanceof RsParenExpr)) {
                warnIfParens(o.getExpr(), RsBundle.message("inspection.message.redundant.parentheses.in.expression"));
            }
        }

        private void warnIfParens(RsExpr expr, String message) {
            if (!(expr instanceof RsParenExpr)) return;
            if (!canWarn((RsParenExpr) expr)) return;
            myHolder.createWeakWarningAnnotation(expr, message, new RemoveRedundantParenthesesFix((RsParenExpr) expr));
        }

        private boolean canWarn(@Nonnull RsParenExpr expr) {
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
