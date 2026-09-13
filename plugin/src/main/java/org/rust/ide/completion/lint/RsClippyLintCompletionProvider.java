/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.completion.lint;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
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
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        super.addCompletions(parameters, context, result);

        RsPath path = PsiTreeUtil.getParentOfType(parameters.getPosition(), RsPath.class);
        if (path == null) return;
        if (getPathPrefix(path).isEmpty()) {
            addLintToCompletion(result, new Lint("clippy", true), getPrefix());
        }
    }
}
