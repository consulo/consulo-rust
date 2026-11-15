/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.it.support;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.matcher.PlainPrefixMatcher;
import consulo.application.util.matcher.PrefixMatcher;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProcess;
import consulo.language.editor.completion.CompletionResult;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.CompletionService;
import consulo.language.editor.completion.CompletionSorter;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The headless platform has no completion service, which every {@link CompletionResultSet} asks for when it is
 * created: this one lets a test hand a result set of its own to a completion provider. There is never a completion
 * process, so nothing here runs a completion.
 */
@Singleton
@ServiceImpl
public class TestCompletionService extends CompletionService {
    @Override
    public void setAdvertisementText(@Nullable String text) {
    }

    @Override
    protected CompletionResultSet createResultSet(CompletionParameters parameters, Consumer<? super CompletionResult> consumer,
                                                  CompletionContributor contributor, PrefixMatcher matcher) {
        throw new UnsupportedOperationException("no completion process in a headless test");
    }

    @Override
    protected String suggestPrefix(CompletionParameters parameters) {
        return "";
    }

    @Override
    protected PrefixMatcher createMatcher(String prefix, boolean typoTolerant) {
        return new PlainPrefixMatcher(prefix);
    }

    @Override
    public @Nullable CompletionProcess getCurrentCompletion() {
        return null;
    }

    @Override
    public CompletionSorter defaultSorter(CompletionParameters parameters, PrefixMatcher matcher) {
        throw new UnsupportedOperationException("no completion process in a headless test");
    }

    @Override
    public CompletionSorter emptySorter() {
        // CompletionSorter.emptySorter() asks this service: there is no sorter to give
        throw new UnsupportedOperationException("no completion process in a headless test");
    }
}
