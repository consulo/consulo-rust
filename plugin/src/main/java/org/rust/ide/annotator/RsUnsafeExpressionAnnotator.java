/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.colors.RsColor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.TyPointer;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.openapiext.OpenApiUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.rust.lang.core.psi.ext.RsBinaryOpUtil;
import org.rust.lang.core.psi.ext.RsConstantUtil;
import org.rust.lang.core.psi.ext.RsPossibleMacroCallUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsMethodCallUtil;

public class RsUnsafeExpressionAnnotator extends AnnotatorBase {

    private static final Set<String> UNSAFE_MACRO_LIST;
    static {
        UNSAFE_MACRO_LIST = new HashSet<>();
        UNSAFE_MACRO_LIST.add("asm");
    }

    @Override
    protected void annotateInternal(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        RsAnnotationHolder rsHolder = new RsAnnotationHolder(holder);
        RsVisitor visitor = new RsVisitor() {
            @Override
            public void visitCallExpr(@Nonnull RsCallExpr o) { checkCall(o, rsHolder); }
            @Override
            public void visitDotExpr(@Nonnull RsDotExpr o) { checkDotExpr(o, rsHolder); }
            @Override
            public void visitPathExpr(@Nonnull RsPathExpr o) { checkPathExpr(o, rsHolder); }
            @Override
            public void visitUnaryExpr(@Nonnull RsUnaryExpr o) { checkUnary(o, rsHolder); }
            @Override
            public void visitMacroExpr(@Nonnull RsMacroExpr o) { checkMacroExpr(o, rsHolder); }
        };

        element.accept(visitor);
    }

    private void annotateUnsafeCall(@Nonnull RsExpr expr, @Nonnull RsAnnotationHolder holder) {
        if (!RsElementUtil.existsAfterExpansion(expr)) return;

        if (RsExprUtil.isInUnsafeContext(expr)) {
            TextRange textRange;
            if (expr instanceof RsCallExpr) {
                RsExpr callee = ((RsCallExpr) expr).getExpr();
                if (callee instanceof RsPathExpr) {
                    textRange = RsPathUtil.getTextRangeOfLastSegment(((RsPathExpr) callee).getPath());
                    if (textRange == null) return;
                } else {
                    textRange = callee.getTextRange();
                }
            } else if (expr instanceof RsDotExpr) {
                RsMethodCall call = ((RsDotExpr) expr).getMethodCall();
                if (call == null) return;
                textRange = RsMethodCallUtil.getTextRangeWithoutValueArguments(call);
            } else {
                return;
            }
            createUnsafeAnnotation(holder.getHolder(), textRange, RsBundle.message("inspection.message.call.to.unsafe.function"));
        } else {
            RsDiagnostic.addToHolder(new RsDiagnostic.UnsafeError(expr, RsBundle.message("inspection.message.call.to.unsafe.function.requires.unsafe.function.or.block")), holder);
        }
    }

    private void annotateUnsafeStaticRef(@Nonnull RsPathExpr expr, @Nonnull RsConstant element, @Nonnull RsAnnotationHolder holder) {
        String constantType;
        if (RsConstantUtil.getKind(element) == RsConstantKind.MUT_STATIC) {
            constantType = RsBundle.message("inspection.message.mutable");
        } else if (RsConstantUtil.getKind(element) == RsConstantKind.STATIC && element.getParent() instanceof RsForeignModItem) {
            constantType = RsBundle.message("inspection.message.extern");
        } else {
            return;
        }

        if (RsExprUtil.isInUnsafeContext(expr)) {
            TextRange textRange = RsPathUtil.getTextRangeOfLastSegment(expr.getPath());
            if (textRange == null) return;
            createUnsafeAnnotation(holder.getHolder(), textRange, RsBundle.message("inspection.message.use.unsafe.static", constantType));
        } else {
            RsDiagnostic.addToHolder(new RsDiagnostic.UnsafeError(expr, RsBundle.message("inspection.message.use.static.unsafe.requires.unsafe.function.or.block", constantType)), holder);
        }
    }

    public void checkDotExpr(@Nonnull RsDotExpr o, @Nonnull RsAnnotationHolder holder) {
        RsMethodCall methodCall = o.getMethodCall();
        if (methodCall != null) {
            PsiElement resolved = methodCall.getReference().resolve();
            if (resolved instanceof RsFunction) {
                RsFunction fn = (RsFunction) resolved;
                if (RsFunctionUtil.isActuallyUnsafe(fn)) {
                    annotateUnsafeCall(o, holder);
                }
            }
        }

        PsiElement exprParent = o.getParent();
        if (exprParent instanceof RsBinaryExpr) {
            RsBinaryExpr binaryExpr = (RsBinaryExpr) exprParent;
            if (RsBinaryOpUtil.getOperatorType(binaryExpr) == AssignmentOp.EQ && binaryExpr.getLeft() == o) return;
        }
        if (o.getFieldLookup() != null) {
            Ty type = RsTypesUtil.getType(o.getExpr());
            if (!(type instanceof TyAdt)) return;
            RsItemElement item = ((TyAdt) type).getItem();
            if (!(item instanceof RsStructItem)) return;
            if (RsStructItemUtil.getKind((RsStructItem) item) == RsStructKind.UNION && !RsExprUtil.isInUnsafeContext(o.getExpr())) {
                RsDiagnostic.addToHolder(new RsDiagnostic.UnsafeError(o, RsBundle.message("inspection.message.access.to.union.field.unsafe.requires.unsafe.function.or.block")), holder);
            }
        }
    }

    public void checkCall(@Nonnull RsCallExpr element, @Nonnull RsAnnotationHolder holder) {
        RsExpr expr = element.getExpr();
        if (!(expr instanceof RsPathExpr)) return;
        RsPath path = ((RsPathExpr) expr).getPath();
        PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
        if (!(resolved instanceof RsFunction)) return;
        RsFunction fn = (RsFunction) resolved;
        if (RsFunctionUtil.isActuallyUnsafe(fn)) {
            annotateUnsafeCall(element, holder);
        }
    }

    public void checkPathExpr(@Nonnull RsPathExpr expr, @Nonnull RsAnnotationHolder holder) {
        PsiElement resolved = expr.getPath().getReference() != null ? expr.getPath().getReference().resolve() : null;
        if (!(resolved instanceof RsConstant)) return;
        annotateUnsafeStaticRef(expr, (RsConstant) resolved, holder);
    }

    public void checkUnary(@Nonnull RsUnaryExpr element, @Nonnull RsAnnotationHolder holder) {
        PsiElement mul = element.getMul();
        if (mul == null) return;
        RsExpr innerExpr = element.getExpr();
        if (innerExpr == null) return;
        Ty type = RsTypesUtil.getType(innerExpr);
        if (!(type instanceof TyPointer)) return;

        if (RsExprUtil.isInUnsafeContext(element)) {
            createUnsafeAnnotation(holder.getHolder(), mul.getTextRange(), RsBundle.message("inspection.message.unsafe.dereference.raw.pointer"));
        } else {
            RsDiagnostic.addToHolder(new RsDiagnostic.UnsafeError(element, RsBundle.message("inspection.message.dereference.raw.pointer.requires.unsafe.function.or.block")), holder);
        }
    }

    public void checkMacroExpr(@Nonnull RsMacroExpr macroExpr, @Nonnull RsAnnotationHolder holder) {
        RsMacroCall macroCall = macroExpr.getMacroCall();
        String macroName = RsMacroCallUtil.getMacroName(macroCall);

        if (UNSAFE_MACRO_LIST.contains(macroName)) {
            RsMacroDefinitionBase macroDef = RsMacroCallUtil.resolveToMacro(macroCall);

            if (macroDef instanceof RsMacro && RsMacroExtUtil.getHasRustcBuiltinMacro((RsMacro) macroDef) && !RsExprUtil.isInUnsafeContext(macroExpr)) {
                RsDiagnostic.addToHolder(new RsDiagnostic.UnsafeError(
                    macroExpr,
                    RsBundle.message("inspection.message.use.unsafe.requires.unsafe.function.or.block", macroName)
                ), holder);
            }
        }
    }

    private void createUnsafeAnnotation(@Nonnull AnnotationHolder holder, @Nonnull TextRange textRange, @Nonnull  String message) {
        if (holder.isBatchMode()) return;
        RsColor color = RsColor.UNSAFE_CODE;
        HighlightSeverity severity = OpenApiUtil.isUnitTestMode() ? color.getTestSeverity() : HighlightSeverity.INFORMATION;

        holder.newAnnotation(severity, message)
            .range(textRange)
            .textAttributes(color.getTextAttributesKey()).create();
    }
}
