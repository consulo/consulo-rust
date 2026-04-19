/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.codeInspection.util.InspectionMessage;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
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
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        RsAnnotationHolder rsHolder = new RsAnnotationHolder(holder);
        RsVisitor visitor = new RsVisitor() {
            @Override
            public void visitCallExpr(@NotNull RsCallExpr o) { checkCall(o, rsHolder); }
            @Override
            public void visitDotExpr(@NotNull RsDotExpr o) { checkDotExpr(o, rsHolder); }
            @Override
            public void visitPathExpr(@NotNull RsPathExpr o) { checkPathExpr(o, rsHolder); }
            @Override
            public void visitUnaryExpr(@NotNull RsUnaryExpr o) { checkUnary(o, rsHolder); }
            @Override
            public void visitMacroExpr(@NotNull RsMacroExpr o) { checkMacroExpr(o, rsHolder); }
        };

        element.accept(visitor);
    }

    private void annotateUnsafeCall(@NotNull RsExpr expr, @NotNull RsAnnotationHolder holder) {
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

    private void annotateUnsafeStaticRef(@NotNull RsPathExpr expr, @NotNull RsConstant element, @NotNull RsAnnotationHolder holder) {
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

    public void checkDotExpr(@NotNull RsDotExpr o, @NotNull RsAnnotationHolder holder) {
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

    public void checkCall(@NotNull RsCallExpr element, @NotNull RsAnnotationHolder holder) {
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

    public void checkPathExpr(@NotNull RsPathExpr expr, @NotNull RsAnnotationHolder holder) {
        PsiElement resolved = expr.getPath().getReference() != null ? expr.getPath().getReference().resolve() : null;
        if (!(resolved instanceof RsConstant)) return;
        annotateUnsafeStaticRef(expr, (RsConstant) resolved, holder);
    }

    public void checkUnary(@NotNull RsUnaryExpr element, @NotNull RsAnnotationHolder holder) {
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

    public void checkMacroExpr(@NotNull RsMacroExpr macroExpr, @NotNull RsAnnotationHolder holder) {
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

    private void createUnsafeAnnotation(@NotNull AnnotationHolder holder, @NotNull TextRange textRange, @NotNull @InspectionMessage String message) {
        if (holder.isBatchMode()) return;
        RsColor color = RsColor.UNSAFE_CODE;
        HighlightSeverity severity = OpenApiUtil.isUnitTestMode() ? color.getTestSeverity() : HighlightSeverity.INFORMATION;

        holder.newAnnotation(severity, message)
            .range(textRange)
            .textAttributes(color.getTextAttributesKey()).create();
    }
}
