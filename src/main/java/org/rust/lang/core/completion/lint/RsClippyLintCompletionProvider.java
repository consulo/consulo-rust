/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion.lint;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPath;

import java.util.List;

public class RsClippyLintCompletionProvider extends RsLintCompletionProvider {
    public static final RsClippyLintCompletionProvider INSTANCE = new RsClippyLintCompletionProvider();

    private RsClippyLintCompletionProvider() {
    }

    @Override
    protected String getPrefix() {
        return "clippy::";
    }

    @Override
    protected List<Lint> getLints() {
        return ClippyLints.CLIPPY_LINTS;
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        super.addCompletions(parameters, context, result);

        RsPath path = PsiTreeUtil.getParentOfType(parameters.getPosition(), RsPath.class);
        if (path == null) return;
        if (getPathPrefix(path).isEmpty()) {
            addLintToCompletion(result, new Lint("clippy", true), getPrefix());
        }
    }
}
