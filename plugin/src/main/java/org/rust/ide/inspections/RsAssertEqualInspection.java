/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.MacroBraces;
import org.rust.lang.core.psi.ext.BinaryOperator;
import org.rust.lang.core.psi.ext.EqualityOp;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsExprUtil;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.openapiext.Testmark;

import java.util.List;
import java.util.stream.Collectors;

public class RsAssertEqualInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMacroCall2(@Nonnull RsMacroCall o) {
                if (!RsMacroCallUtil.getMacroName(o).equals("assert")) return;
                RsAssertMacroArgument assertMacroArg = o.getAssertMacroArgument();
                if (assertMacroArg == null) return;

                RsExpr exprRaw = assertMacroArg.getExpr();
                if (!(exprRaw instanceof RsBinaryExpr)) return;
                RsBinaryExpr expr = (RsBinaryExpr) exprRaw;

                String macroName;
                String operator;
                BinaryOperator opType = RsExprUtil.getOperatorType(expr);
                if (opType == EqualityOp.EQ) {
                    macroName = "assert_eq";
                    operator = "==";
                } else if (opType == EqualityOp.EXCLEQ) {
                    macroName = "assert_ne";
                    operator = "!=";
                } else {
                    return;
                }

                if (!isExprSuitable(expr)) return;

                holder.registerProblem(
                    o,
                    RsBundle.message("inspection.message.assert.b.can.be.b", operator, macroName),
                    new SpecializedAssertQuickFix(o, macroName)
                );
            }

            private boolean isExprSuitable(@Nonnull RsBinaryExpr expr) {
                Ty leftType = RsTypesUtil.getType(expr.getLeft());
                RsExpr right = expr.getRight();
                if (right == null) return false;
                Ty rightType = RsTypesUtil.getType(right);

                ImplLookup lookup = ImplLookup.relativeTo(expr);
                if (!lookup.isDebug(leftType).isTrue() || !lookup.isDebug(rightType).isTrue()) {
                    Testmarks.DebugTraitIsNotImplemented.hit();
                    return false;
                }
                if (!lookup.isPartialEq(leftType, rightType).isTrue()) {
                    Testmarks.PartialEqTraitIsNotImplemented.hit();
                    return false;
                }
                return true;
            }
        };
    }

    private static class SpecializedAssertQuickFix extends RsQuickFixBase<RsMacroCall> {
        private final String assertName;

        SpecializedAssertQuickFix(@Nonnull RsMacroCall element, @Nonnull String assertName) {
            super(element);
            this.assertName = assertName;
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.convert.to.macro", assertName));
            }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return getText();
            }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsMacroCall element) {
            RsAssertMacroArgument assertArg = element.getAssertMacroArgument();
            if (assertArg == null) return;

            Pair<RsExpr, RsExpr> args = comparedAssertArgs(assertArg);
            if (args == null) return;
            RsExpr left = args.getFirst();
            RsExpr right = args.getSecond();
            List<RsFormatMacroArg> formatArgs = assertArg.getFormatMacroArgList();
            String appendix;
            if (!formatArgs.isEmpty()) {
                appendix = formatArgs.stream()
                    .map(arg -> arg.getText())
                    .collect(Collectors.joining(", ", ",", ""));
            } else {
                appendix = "";
            }
            MacroBraces bracesKind = RsMacroCallUtil.getBracesKind(element);
            if (bracesKind == null) return;
            RsMacroCall newAssert = new RsPsiFactory(project).createMacroCall(
                RsExpandedElementUtil.getExpansionContext(element),
                bracesKind,
                assertName,
                left.getText() + ", " + right.getText() + appendix
            );
            element.replace(newAssert);
        }

        @Nullable
        private Pair<RsExpr, RsExpr> comparedAssertArgs(@Nonnull RsAssertMacroArgument arg) {
            RsExpr exprRaw = arg.getExpr();
            if (!(exprRaw instanceof RsBinaryExpr)) return null;
            RsBinaryExpr expr = (RsBinaryExpr) exprRaw;
            RsExpr right = expr.getRight();
            if (right == null) return null;
            return Pair.create(expr.getLeft(), right);
        }
    }

    public static class Testmarks {
        public static final Testmark PartialEqTraitIsNotImplemented = new Testmark();
        public static final Testmark DebugTraitIsNotImplemented = new Testmark();
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.assert.equal.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
