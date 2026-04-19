/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceVariable;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiParserFacade;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsInPlaceVariableIntroducer;
import org.rust.ide.refactoring.RsNameSuggestions;
import org.rust.ide.refactoring.SuggestedNames;
import org.rust.ide.utils.PsiUtils;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.rust.ide.refactoring.ExtraxtExpressionUtils.*;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsStmtUtil;

public final class IntroduceVariableImpl {

    private IntroduceVariableImpl() {
    }

    public static void extractExpression(
        @NotNull Editor editor,
        @NotNull RsExpr expr,
        boolean postfixLet,
        @NlsContexts.Command @NotNull String commandName
    ) {
        if (!expr.isValid()) return;
        List<RsExpr> occurrences = findOccurrences(expr);
        org.rust.ide.refactoring.ExtraxtExpressionUiUtils.showOccurrencesChooser(editor, expr, occurrences, occurrencesToReplace -> {
            new ExpressionReplacer(expr.getProject(), editor, expr)
                .replaceElementForAllExpr(occurrencesToReplace, postfixLet, commandName);
        });
    }

    private static class ExpressionReplacer {
        @NotNull
        private final Project project;
        @NotNull
        private final Editor editor;
        @NotNull
        private final RsExpr chosenExpr;
        @NotNull
        private final RsPsiFactory psiFactory;
        @NotNull
        private final SuggestedNames suggestedNames;

        ExpressionReplacer(@NotNull Project project, @NotNull Editor editor, @NotNull RsExpr chosenExpr) {
            this.project = project;
            this.editor = editor;
            this.chosenExpr = chosenExpr;
            this.psiFactory = new RsPsiFactory(project);
            this.suggestedNames = RsNameSuggestions.suggestedNames(chosenExpr);
        }

        void replaceElementForAllExpr(
            @NotNull List<RsExpr> exprs,
            boolean postfixLet,
            @NlsContexts.Command @NotNull String commandName
        ) {
            PsiElement anchor = findAnchor(exprs, chosenExpr);
            if (anchor == null) return;
            List<RsExpr> sortedExprs = new ArrayList<>(exprs);
            sortedExprs.sort(Comparator.comparingInt(PsiElement::getStartOffsetInParent));
            RsExpr firstExpr = sortedExprs.isEmpty() ? chosenExpr : sortedExprs.get(0);

            // `inlinableExprStmt` is the element that should be replaced with the new let binding.
            RsExprStmt inlinableExprStmt = null;
            if (firstExpr.getParent() instanceof RsExprStmt) {
                RsExprStmt stmt = (RsExprStmt) firstExpr.getParent();
                if (stmt == anchor && (!RsStmtUtil.isTailStmt(stmt) || postfixLet)) {
                    inlinableExprStmt = stmt;
                }
            }

            RsLetDecl let = createLet(suggestedNames.getDefault());
            RsExpr name = psiFactory.createExpression(suggestedNames.getDefault());

            RsExprStmt finalInlinableExprStmt = inlinableExprStmt;
            org.rust.openapiext.OpenApiUtil.runWriteCommandAction(project, commandName, () -> {
                PsiElement letBinding;
                if (finalInlinableExprStmt != null) {
                    letBinding = finalInlinableExprStmt.replace(let);
                    for (int i = 1; i < sortedExprs.size(); i++) {
                        sortedExprs.get(i).replace(name);
                    }
                } else {
                    RsLambdaExpr parentLambda = RsElementUtil.ancestorStrict(chosenExpr, RsLambdaExpr.class);
                    boolean lambdaMayBeAnchor = parentLambda != null && sortedExprs.stream().allMatch(e -> PsiTreeUtil.isAncestor(parentLambda, e, false));
                    letBinding = introduceLet(anchor, let);
                    for (RsExpr e : sortedExprs) {
                        e.replace(name);
                    }
                    if (lambdaMayBeAnchor) {
                        letBinding = moveIntoLambdaBlockIfNeeded(letBinding, parentLambda);
                    }
                }

                PsiElement nameElem = moveEditorToNameElement(editor, letBinding);

                if (nameElem != null) {
                    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
                    new RsInPlaceVariableIntroducer(
                        (com.intellij.psi.PsiNamedElement) nameElem, editor, project,
                        RsBundle.message("command.name.choose.variable")
                    ).performInplaceRefactoring(suggestedNames.getAll());
                }
            });
        }

        @NotNull
        private RsLetDecl createLet(@NotNull String name) {
            PsiElement parent = chosenExpr.getParent();
            boolean mutable = parent instanceof RsUnaryExpr && ((RsUnaryExpr) parent).getMut() != null;
            return psiFactory.createLetDeclaration(name, chosenExpr, mutable, null);
        }

        @NotNull
        private PsiElement introduceLet(@NotNull PsiElement anchor, @NotNull RsLetDecl let) {
            PsiElement context = anchor.getParent();
            PsiElement newline = PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n");
            PsiElement result = context.addBefore(let, anchor);
            context.addAfter(newline, result);
            return result;
        }

        @Nullable
        private PsiElement moveIntoLambdaBlockIfNeeded(@NotNull PsiElement element, @Nullable RsLambdaExpr lambda) {
            if (lambda == null) return element;
            RsExpr body = lambda.getExpr();
            if (body == null) return element;
            if (body instanceof RsBlockExpr) return element;
            RsBlockExpr blockExpr = (RsBlockExpr) body.replace(psiFactory.createBlockExpr("\n" + body.getText() + "\n"));
            RsBlock block = blockExpr.getBlock();
            PsiElement result = block.addBefore(element, RsBlockUtil.getSyntaxTailStmt(block));
            element.delete();
            return result;
        }
    }

    /**
     * An anchor point is surrounding element before the block scope, which is used to scope
     * the insertion of the new let binding.
     */
    @Nullable
    private static PsiElement findAnchor(@NotNull List<? extends PsiElement> exprs, @NotNull RsExpr chosenExpr) {
        List<PsiElement> allElements = new ArrayList<>(exprs);
        allElements.add(chosenExpr);
        PsiElement commonParent = PsiTreeUtil.findCommonParent(allElements);
        if (commonParent == null) return null;

        PsiElement firstExpr = exprs.stream()
            .min(Comparator.comparingInt(PsiElement::getStartOffsetInParent))
            .map(e -> (PsiElement) e)
            .orElse(chosenExpr);

        RsBlock block = RsElementUtil.ancestorOrSelf(commonParent, RsBlock.class);
        if (block == null) return null;

        return PsiUtils.getTopmostParentInside(firstExpr, block);
    }
}
