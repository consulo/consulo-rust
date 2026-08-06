/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsAttrOwnerExtUtil;
import org.rust.lang.core.psi.ext.RsBlockUtil;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.*;

import java.util.ArrayList;
import java.util.List;
import org.rust.ide.annotator.FunctionCallContextUtil;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.ide.utils.template.EditorExtUtil;

/** Analogue of rustc's unused_must_use. See also {@link RsDoubleMustUseInspection}. */
public class RsUnusedMustUseInspection extends RsLintInspection {

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.UnusedMustUse;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitExprStmt(@Nonnull RsExprStmt o) {
                super.visitExprStmt(o);
                PsiElement parent = o.getParent();
                if (parent instanceof RsBlock) {
                    // Ignore if o is actually a tail expr
                    RsExpr tailExpr = RsBlockUtil.getExpandedStmtsAndTailExpr((RsBlock) parent).getSecond();
                    if (o.getExpr() == tailExpr) return;
                }
                InspectionResult problem = inspectAndProposeFixes(o.getExpr());
                if (problem != null) {
                    registerLintProblem(holder, o.getExpr(), problem.myDescription, RsLintHighlightingType.WEAK_WARNING, problem.myFixes);
                }
            }
        };
    }

    @Nullable
    private static InspectionResult inspectAndProposeFixes(@Nonnull RsExpr expr) {
        List<LocalQuickFix> fixes = new ArrayList<>();
        RsFunction function = null;
        if (expr instanceof RsDotExpr) {
            RsMethodCall methodCall = ((RsDotExpr) expr).getMethodCall();
            if (methodCall != null) {
                org.rust.ide.annotator.RsErrorAnnotator.FunctionCallContext ctx = FunctionCallContextUtil.getFunctionCallContext(methodCall);
                function = ctx != null ? ctx.getFunction() : null;
            }
        } else if (expr instanceof RsCallExpr) {
            org.rust.ide.annotator.RsErrorAnnotator.FunctionCallContext ctx = FunctionCallContextUtil.getFunctionCallContext((RsCallExpr) expr);
            function = ctx != null ? ctx.getFunction() : null;
        }
        if (isAsyncHardcodedProcMacro(function)) return null;
        String description = checkTypeMustUse(RsTypesUtil.getType(expr), expr, fixes);
        if (description == null) {
            description = checkFuncMustUse(function);
        }
        if (description == null) return null;
        if (returnsStdResult(expr)) {
            fixes.add(new FixAddExpect(expr));
            fixes.add(new FixAddUnwrap(expr));
        }
        fixes.add(new FixAddLetUnderscore(expr));
        return new InspectionResult(description, fixes);
    }

    private static boolean returnsStdResult(@Nonnull RsExpr expr) {
        Ty type = RsTypesUtil.getType(expr);
        if (!(type instanceof TyAdt)) return false;
        return ((TyAdt) type).getItem() == KnownItems.getKnownItems(expr).getResult();
    }

    // `#[tokio::main] async fn main() { ... }` actually doesn't return `Future`
    private static boolean isAsyncHardcodedProcMacro(@Nullable RsFunction function) {
        if (function == null) return false;
        List<KnownProcMacroKind> hardcodedProcMacros = ProcMacroAttribute.getHardcodedProcMacroAttributes(function);
        for (KnownProcMacroKind kind : hardcodedProcMacros) {
            if (kind == KnownProcMacroKind.ASYNC_MAIN || kind == KnownProcMacroKind.ASYNC_TEST || kind == KnownProcMacroKind.ASYNC_BENCH) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private static String checkFuncMustUse(@Nullable RsFunction function) {
        if (function == null || !hasMustUseAttr(function)) return null;
        return RsBundle.message("inspection.UnusedMustUse.description.function.attribute", String.valueOf(function.getName()));
    }

    @Nullable
    private static String checkTypeMustUse(@Nonnull Ty type, @Nonnull RsExpr expr, @Nonnull List<LocalQuickFix> fixes) {
        String typeText = null;
        if (type instanceof TyAdt) {
            TyAdt adt = (TyAdt) type;
            // Box<dyn Trait>
            if (adt.getItem() == KnownItems.getKnownItems(expr).getBox()) {
                Ty inner = !adt.getTypeArguments().isEmpty() ? adt.getTypeArguments().get(0) : null;
                if (inner == null) return null;
                return checkTypeMustUse(inner, expr, fixes);
            }
            if (hasMustUseAttr(adt.getItem())) {
                Ty futureTy = RsTypesUtil.getImplLookup(expr).lookupFutureOutputTy(adt, true).getValue();
                if (!(futureTy instanceof TyUnknown)) {
                    fixes.add(new AddAwaitFix(expr));
                }
                typeText = type.toString();
            }
        } else if (type instanceof TyTraitObject) {
            // dyn Trait
            List<org.rust.lang.core.types.BoundElement<RsTraitItem>> traits = ((TyTraitObject) type).getTraits();
            RsTraitItem trait = !traits.isEmpty() ? traits.get(0).getTypedElement() : null;
            if (trait == null || !hasMustUseAttr(trait)) return null;
            if (trait == KnownItems.getKnownItems(expr).getFuture()) {
                fixes.add(new AddAwaitFix(expr));
            }
            typeText = "dyn " + trait.getName();
        } else if (type instanceof TyAnon) {
            // impl Trait
            for (org.rust.lang.core.types.BoundElement<RsTraitItem> traitBound : ((TyAnon) type).getTraits()) {
                if (hasMustUseAttr(traitBound.getTypedElement())) {
                    if (traitBound.getTypedElement() == KnownItems.getKnownItems(expr).getFuture()) {
                        fixes.add(new AddAwaitFix(expr));
                    }
                    typeText = "impl " + traitBound.getTypedElement().getName();
                    break;
                }
            }
        }
        if (typeText == null) return null;
        return RsBundle.message("inspection.UnusedMustUse.description.type.attribute", typeText);
    }

    private static boolean hasMustUseAttr(@Nonnull RsItemElement item) {
        return RsAttrOwnerExtUtil.findFirstMetaItem(item, "must_use") != null;
    }

    private static class InspectionResult {
        final String myDescription;
        final List<LocalQuickFix> myFixes;

        InspectionResult(@Nonnull String description, @Nonnull List<LocalQuickFix> fixes) {
            myDescription = description;
            myFixes = fixes;
        }
    }

    private static class FixAddLetUnderscore extends RsQuickFixBase<RsExpr> {

        FixAddLetUnderscore(@Nonnull RsExpr anchor) {
            super(anchor);
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("inspection.UnusedMustUse.FixAddLetUnderscore.name"));
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return getFamilyName();
        }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
            RsLetDecl letExpr = new RsPsiFactory(project).createLetDeclaration("_", element);
            RsLetDecl newLetExpr = (RsLetDecl) element.getParent().replace(letExpr);
            RsPat pat = newLetExpr.getPat();
            if (pat == null || editor == null) return;
            org.rust.ide.utils.template.RsTemplateBuilder tpl = EditorExtUtil.newTemplateBuilder(editor, newLetExpr);
            if (tpl == null) return;
            tpl.replaceElement(pat, (String) null);
            tpl.runInline();
        }
    }

    private static class AddAwaitFix extends RsQuickFixBase<RsExpr> {

        AddAwaitFix(@Nonnull RsExpr element) {
            super(element);
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("inspection.UnusedMustUse.AddAwaitFix.name"));
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return getFamilyName();
        }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
            element.replace(new RsPsiFactory(project).createExpression(element.getText() + ".await"));
        }
    }

    private static class FixAddUnwrap extends RsQuickFixBase<RsExpr> {

        FixAddUnwrap(@Nonnull RsExpr element) {
            super(element);
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("inspection.UnusedMustUse.FixAddUnwrap.name"));
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return getFamilyName();
        }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
            element.replace(new RsPsiFactory(project).createExpression(element.getText() + ".unwrap()"));
        }
    }

    private static class FixAddExpect extends RsQuickFixBase<RsExpr> {

        FixAddExpect(@Nonnull RsExpr anchor) {
            super(anchor);
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getFamilyName() {
            return consulo.localize.LocalizeValue.of(RsBundle.message("inspection.UnusedMustUse.FixAddExpect.family.name"));
        }

        @Nonnull
        @Override
        public consulo.localize.LocalizeValue getText() {
            return getFamilyName();
        }

        @Override
        public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
            RsExpr dotExprRaw = new RsPsiFactory(project).createExpression(element.getText() + ".expect(\"TODO: panic message\")");
            RsDotExpr newDotExpr = (RsDotExpr) element.replace(dotExprRaw);
            RsMethodCall methodCall = newDotExpr.getMethodCall();
            if (methodCall == null) return;
            RsValueArgumentList argList = methodCall.getValueArgumentList();
            List<RsExpr> exprList = argList.getExprList();
            if (exprList.size() != 1) return;
            RsLitExpr stringLiteral = (RsLitExpr) exprList.get(0);
            RsLiteralKind kind = RsLiteralKindUtil.getKind(stringLiteral);
            if (!(kind instanceof RsLiteralKind.StringLiteral)) return;
            consulo.document.util.TextRange rangeWithoutQuotes = ((RsLiteralKind.StringLiteral) kind).getOffsets().getValue();
            if (rangeWithoutQuotes == null || editor == null) return;
            org.rust.ide.utils.template.RsTemplateBuilder tpl = EditorExtUtil.newTemplateBuilder(editor, newDotExpr);
            if (tpl == null) return;
            tpl.replaceElement(stringLiteral, rangeWithoutQuotes, "TODO: panic message");
            tpl.runInline();
        }
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.unused.must.use.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("lints"));
    }
}
