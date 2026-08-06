/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceConstant;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiParserFacade;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsInPlaceVariableIntroducer;
import org.rust.ide.refactoring.RsNameSuggestions;
import org.rust.ide.refactoring.SuggestedNames;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsConstantUtil;
import org.rust.openapiext.NonBlockingUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import java.util.stream.Collectors;

import static org.rust.ide.refactoring.ExtraxtExpressionUtils.*;
import static org.rust.ide.refactoring.ExtraxtExpressionUiUtils.*;

public class RsIntroduceConstantHandler implements RefactoringActionHandler {

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nullable DataContext dataContext) {
        if (!(file instanceof RsFile)) return;
        List<RsExpr> exprs = findCandidateExpressionsToExtract(editor, (RsFile) file);

        // isExtractable uses resolve, so we must not call it from EDT
        NonBlockingUtil.nonBlocking(
            project,
            () -> exprs.stream().filter(RsIntroduceConstantHandler::isExtractable).collect(Collectors.toList()),
            filtered -> {
                if (!editor.isDisposed()) {
                    handleExpressions(project, editor, filtered);
                }
            }
        );
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiElement [] elements, @Nullable DataContext dataContext) {
        // this doesn't get called from the editor.
    }

    private void handleExpressions(@Nonnull Project project, @Nonnull Editor editor, @Nonnull List<RsExpr> exprs) {
        switch (exprs.size()) {
            case 0: {
                String message = RefactoringBundle.message(editor.getSelectionModel().hasSelection()
                    ? "selected.block.should.represent.an.expression"
                    : "refactoring.introduce.selection.error"
                );
                String title = RefactoringBundle.message("introduce.constant.title");
                String helpId = "refactoring.extractConstant";
                CommonRefactoringUtil.showErrorHint(project, editor, message, title, helpId);
                break;
            }
            case 1:
                extractExpression(editor, exprs.get(0));
                break;
            default:
                showExpressionChooser(editor, exprs, expr -> extractExpression(editor, expr));
                break;
        }
    }

    private static boolean isExtractable(@Nonnull RsExpr expr) {
        if (expr instanceof RsLitExpr) {
            return true;
        }
        if (expr instanceof RsBinaryExpr) {
            RsBinaryExpr binary = (RsBinaryExpr) expr;
            boolean leftOk = isExtractable(binary.getLeft());
            RsExpr right = binary.getRight();
            boolean rightOk = right == null || isExtractable(right);
            return leftOk && rightOk;
        }
        if (expr instanceof RsPathExpr) {
            RsPathExpr pathExpr = (RsPathExpr) expr;
            PsiElement resolved = pathExpr.getPath().getReference() != null
                ? pathExpr.getPath().getReference().resolve()
                : null;
            if (resolved instanceof RsConstant) {
                return RsConstantUtil.isConst((RsConstant) resolved);
            }
            return false;
        }
        return false;
    }

    /**
     * This cannot be called from EDT, because it uses resolve.
     */
    @Nonnull
    private static Set<String> findExistingBindings(@Nonnull InsertionCandidate candidate, @Nonnull List<RsExpr> occurrences) {
        PsiElement owner = candidate.getParent();
        PsiElement firstChild = owner.getChildren().length > 0 ? owner.getChildren()[0] : null;
        Set<String> bindings = new HashSet<>();
        if (firstChild instanceof RsElement) {
            Set<String> allVisible = RsElementUtil.getAllVisibleBindings((RsElement) firstChild);
            if (allVisible != null) {
                bindings.addAll(allVisible);
            }
        }
        for (RsExpr occ : occurrences) {
            Map<String, ?> localBindings = RsElementUtil.getLocalVariableVisibleBindings(occ);
            if (localBindings != null) {
                bindings.addAll(localBindings.keySet());
            }
        }
        return bindings;
    }

    private static void replaceWithConstant(
        @Nonnull RsExpr expr,
        @Nonnull List<RsExpr> occurrences,
        @Nonnull InsertionCandidate candidate,
        @Nonnull Set<String> existingBindings,
        @Nonnull Editor editor
    ) {
        Project project = expr.getProject();
        RsPsiFactory factory = new RsPsiFactory(project);
        SuggestedNames suggestedNames = RsNameSuggestions.suggestedNames(expr);

        String name = null;
        for (String suggested : suggestedNames.getAll()) {
            String upper = suggested.toUpperCase();
            if (!existingBindings.contains(upper)) {
                name = upper;
                break;
            }
        }
        if (name == null) {
            name = RsNameSuggestions.freshenName(
                suggestedNames.getDefault().toUpperCase(), existingBindings
            );
        }

        RsConstant constant = factory.createConstant(name, expr);

        String finalName = name;
        org.rust.openapiext.OpenApiUtil.runWriteCommandAction(project, RefactoringBundle.message("introduce.constant.title"), () -> {
            PsiElement newline = PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n");
            PsiElement context = candidate.getParent();
            RsConstant insertedConstant = (RsConstant) context.addBefore(constant, candidate.getAnchor());
            context.addAfter(newline, insertedConstant);
            List<PsiElement> replaced = new ArrayList<>();
            for (RsExpr occ : occurrences) {
                RsExpr created = factory.createExpression(finalName);
                RsPathExpr element = (RsPathExpr) occ.replace(created);
                if (element.getPath().getReference() == null || element.getPath().getReference().resolve() == null) {
                    RsImportHelper.importElement(element, insertedConstant);
                }
                replaced.add(element);
            }

            if (insertedConstant.getIdentifier() == null || insertedConstant.getIdentifier().getTextRange() == null) {
                throw new IllegalStateException("Impossible because we just created a constant with a name");
            }
            editor.getCaretModel().moveToOffset(insertedConstant.getIdentifier().getTextRange().getStartOffset());

            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
            new RsInPlaceVariableIntroducer(insertedConstant, editor, project, RsBundle.message("command.name.choose.constant.name"), replaced)
                .performInplaceRefactoring(new LinkedHashSet<>(Collections.singletonList(finalName)));
        });
    }

    private static void extractExpression(@Nonnull Editor editor, @Nonnull RsExpr expr) {
        if (!expr.isValid()) return;
        List<RsExpr> occurrences = findOccurrences(expr);
        showOccurrencesChooser(editor, expr, occurrences, occurrencesToReplace ->
            IntroduceConstantUiUtils.showInsertionChooser(editor, expr, candidate -> {
                Project project = editor.getProject();
                if (project == null) return;
                NonBlockingUtil.nonBlocking(
                    project,
                    () -> findExistingBindings(candidate, occurrences),
                    bindings -> replaceWithConstant(expr, occurrencesToReplace, candidate, bindings, editor)
                );
            })
        );
    }
}
