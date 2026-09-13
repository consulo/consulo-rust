/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

public class CargoTomlSpecificDependencyHeaderCompletionProvider implements CompletionProvider {
    private final CompletionProvider myLocalCompletionProvider = new LocalCargoTomlSpecificDependencyHeaderCompletionProvider();
    private final CompletionProvider myCratesIoCompletionProvider = new CratesIoCargoTomlSpecificDependencyHeaderCompletionProvider();

    @Nonnull
    private CompletionProvider getDelegate() {
        if (OpenApiUtil.isFeatureEnabled(RsExperiments.CRATES_LOCAL_INDEX)) {
            return myLocalCompletionProvider;
        } else {
            return myCratesIoCompletionProvider;
        }
    }

    @Override
    public void addCompletions(@Nonnull CompletionParameters parameters,
                                  @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        getDelegate().addCompletions(parameters, context, result);
    }
}
