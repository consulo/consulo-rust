/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceVariable;

import consulo.codeEditor.Editor;
import consulo.project.Project;

import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiParserFacade;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.lang.core.psi.ext.impl.RsStmtUtil;
import consulo.language.psi.PsiNamedElement;
import org.rust.ide.refactoring.ExtraxtExpressionUiUtils;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.impl.*;

public final class IntroduceVariableImpl {

    private IntroduceVariableImpl() {
    }

    public static void extractExpression(
        @Nonnull Editor editor,
        @Nonnull RsExpr expr,
        boolean postfixLet,
         @Nonnull String commandName
    ) {
        if (!expr.isValid()) return;
        List<RsExpr> occurrences = findOccurrences(expr);
        org.rust.ide.refactoring.ExtraxtExpressionUiUtils.showOccurrencesChooser(editor, expr, occurrences, occurrencesToReplace -> {
            new ExpressionReplacer(expr.getProject(), editor, expr)
                .replaceElementForAllExpr(occurrencesToReplace, postfixLet, commandName);
        });
    }

    private static class ExpressionReplacer {
        @Nonnull
        private final Project project;
        @Nonnull
        private final Editor editor;
        @Nonnull
        private final RsExpr chosenExpr;
        @Nonnull
        private final RsPsiFactory psiFactory;
        @Nonnull
        private final SuggestedNames suggestedNames;

        ExpressionReplacer(@Nonnull Project project, @Nonnull Editor editor, @Nonnull RsExpr chosenExpr) {
            this.project = project;
            this.editor = editor;
            this.chosenExpr = chosenExpr;
            this.psiFactory = new RsPsiFactory(project);
            this.suggestedNames = RsNameSuggestions.suggestedNames(chosenExpr);
        }

        void replaceElementForAllExpr(
            @Nonnull List<RsExpr> exprs,
            boolean postfixLet,
             @Nonnull String commandName
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
                        (consulo.language.psi.PsiNamedElement) nameElem, editor, project,
                        RsBundle.message("command.name.choose.variable")
                    ).performInplaceRefactoring(suggestedNames.getAll());
                }
            });
        }

        @Nonnull
        private RsLetDecl createLet(@Nonnull String name) {
            PsiElement parent = chosenExpr.getParent();
            boolean mutable = parent instanceof RsUnaryExpr && ((RsUnaryExpr) parent).getMut() != null;
            return psiFactory.createLetDeclaration(name, chosenExpr, mutable, null);
        }

        @Nonnull
        private PsiElement introduceLet(@Nonnull PsiElement anchor, @Nonnull RsLetDecl let) {
            PsiElement context = anchor.getParent();
            PsiElement newline = PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n");
            PsiElement result = context.addBefore(let, anchor);
            context.addAfter(newline, result);
            return result;
        }

        @Nullable
        private PsiElement moveIntoLambdaBlockIfNeeded(@Nonnull PsiElement element, @Nullable RsLambdaExpr lambda) {
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
    private static PsiElement findAnchor(@Nonnull List<? extends PsiElement> exprs, @Nonnull RsExpr chosenExpr) {
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
