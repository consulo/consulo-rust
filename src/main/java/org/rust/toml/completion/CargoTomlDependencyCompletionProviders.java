/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

class CargoTomlDependencyCompletionProvider extends CompletionProvider<CompletionParameters> {
    private final CompletionProvider<CompletionParameters> myLocalCompletionProvider = new LocalCargoTomlDependencyCompletionProvider();
    private final CompletionProvider<CompletionParameters> myCratesIoCompletionProvider = new CratesIoCargoTomlDependencyCompletionProvider();

    @NotNull
    private CompletionProvider<CompletionParameters> getDelegate() {
        if (OpenApiUtil.isFeatureEnabled(RsExperiments.CRATES_LOCAL_INDEX)) {
            return myLocalCompletionProvider;
        } else {
            return myCratesIoCompletionProvider;
        }
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        getDelegate().addCompletionVariants(parameters, context, result);
    }
}
