/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlKeyValue;

import java.util.List;
import java.util.stream.Collectors;

public class CargoTomlKnownValuesCompletionProvider extends CompletionProvider<CompletionParameters> {
    private final List<String> myKnownValues;

    public CargoTomlKnownValuesCompletionProvider(@NotNull List<String> knownValues) {
        myKnownValues = knownValues;
    }

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        TomlKeyValue keyValue = Util.getClosestKeyValueAncestor(parameters.getPosition());
        if (keyValue == null) return;
        result.addAllElements(
            myKnownValues.stream()
                .map(v -> LookupElementBuilder.create(v).withInsertHandler(new Util.StringValueInsertionHandler(keyValue)))
                .collect(Collectors.toList())
        );
    }
}
