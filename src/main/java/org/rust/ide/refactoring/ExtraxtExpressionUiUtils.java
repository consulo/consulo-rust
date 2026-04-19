/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.Pass;
import com.intellij.refactoring.IntroduceTargetChooser;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.introduce.inplace.OccurrencesChooser;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
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
        @NotNull Editor editor,
        @NotNull List<RsExpr> exprs,
        @NotNull Consumer<RsExpr> callback
    ) {
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            callback.accept(MOCK.chooseTarget(exprs));
        } else {
            IntroduceTargetChooser.showChooser(editor, exprs, asPass(callback), rsExpr -> rsExpr.getText());
        }
    }

    public static void showOccurrencesChooser(
        @NotNull Editor editor,
        @NotNull RsExpr expr,
        @NotNull List<RsExpr> occurrences,
        @NotNull Consumer<List<RsExpr>> callback
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
        @NotNull Project project,
        @NotNull Editor editor,
        @NotNull @NlsContexts.DialogMessage String message
    ) {
        String title = RefactoringBundle.message("introduce.parameter.title");
        String helpId = "refactoring.extractParameter";
        CommonRefactoringUtil.showErrorHint(project, editor, message, title, helpId);
    }

    @TestOnly
    public static void withMockTargetExpressionChooser(@NotNull ExtractExpressionUi mock, @NotNull Runnable f) {
        MOCK = mock;
        try {
            f.run();
        } finally {
            MOCK = null;
        }
    }

    @NotNull
    private static <T> Pass<T> asPass(@NotNull Consumer<T> consumer) {
        return new Pass<T>() {
            @Override
            public void pass(T t) {
                consumer.accept(t);
            }
        };
    }
}
