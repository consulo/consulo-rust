/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.codeEditor.Editor;
import consulo.project.Project;

import consulo.language.editor.Pass;
import consulo.language.editor.refactoring.introduce.IntroduceTargetChooser;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.introduce.inplace.OccurrencesChooser;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsFunction;
import org.rust.openapiext.OpenApiUtil;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ExtraxtExpressionUiUtils {
    @Nullable
    public static ExtractExpressionUi MOCK = null;

    private ExtraxtExpressionUiUtils() {
    }

    public static void showExpressionChooser(
        @Nonnull Editor editor,
        @Nonnull List<RsExpr> exprs,
        @Nonnull Consumer<RsExpr> callback
    ) {
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            callback.accept(MOCK.chooseTarget(exprs));
        } else {
            IntroduceTargetChooser.showChooser(editor, exprs, asPass(callback), rsExpr -> rsExpr.getText());
        }
    }

    public static void showOccurrencesChooser(
        @Nonnull Editor editor,
        @Nonnull RsExpr expr,
        @Nonnull List<RsExpr> occurrences,
        @Nonnull Consumer<List<RsExpr>> callback
    ) {
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode() && occurrences.size() > 1) {
            callback.accept(MOCK.chooseOccurrences(expr, occurrences));
        } else {
            OccurrencesChooser.<RsExpr>simpleChooser(editor)
                .showChooser(
                    expr,
                    occurrences,
                    asPass((OccurrencesChooser.ReplaceChoice choice) -> {
                        List<RsExpr> toReplace = choice == OccurrencesChooser.ReplaceChoice.ALL
                            ? occurrences
                            : Collections.singletonList(expr);
                        callback.accept(toReplace);
                    })
                );
        }
    }

    public static void showErrorMessageForExtractParameter(
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull  String message
    ) {
        String title = RefactoringBundle.message("introduce.parameter.title");
        String helpId = "refactoring.extractParameter";
        CommonRefactoringUtil.showErrorHint(project, editor, message, title, helpId);
    }

    
    public static void withMockTargetExpressionChooser(@Nonnull ExtractExpressionUi mock, @Nonnull Runnable f) {
        MOCK = mock;
        try {
            f.run();
        } finally {
            MOCK = null;
        }
    }

    @Nonnull
    private static <T> java.util.function.Consumer<T> asPass(@Nonnull Consumer<T> consumer) {
        return consumer::accept;
    }
}
