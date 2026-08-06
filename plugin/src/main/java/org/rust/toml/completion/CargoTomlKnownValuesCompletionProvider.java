/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlKeyValue;

import java.util.List;
import java.util.stream.Collectors;

public class CargoTomlKnownValuesCompletionProvider implements CompletionProvider {
    private final List<String> myKnownValues;

    public CargoTomlKnownValuesCompletionProvider(@Nonnull List<String> knownValues) {
        myKnownValues = knownValues;
    }

    @Override
    public void addCompletions(@Nonnull CompletionParameters parameters,
                                  @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        TomlKeyValue keyValue = Util.getClosestKeyValueAncestor(parameters.getPosition());
        if (keyValue == null) return;
        result.addAllElements(
            myKnownValues.stream()
                .map(v -> LookupElementBuilder.create(v).withInsertHandler(new Util.StringValueInsertionHandler(keyValue)))
                .collect(Collectors.toList())
        );
    }
}
