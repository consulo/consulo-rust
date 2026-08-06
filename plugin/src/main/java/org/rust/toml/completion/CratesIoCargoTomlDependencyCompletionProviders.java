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
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlKeySegment;
import org.toml.lang.psi.TomlKeyValue;
import org.toml.lang.psi.TomlTable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see org.rust.toml.CargoTomlPsiPattern#getInDependencyKeyValue()
 */
class CratesIoCargoTomlDependencyCompletionProvider extends TomlKeyValueCompletionProviderBase {
    @Override
    protected void completeKey(@Nonnull TomlKeyValue keyValue, @Nonnull CompletionResultSet result) {
        List<TomlKeySegment> segments = keyValue.getKey().getSegments();
        if (segments.size() != 1) return;
        TomlKeySegment key = segments.get(0);
        Collection<CratesIoApi.CrateDescription> crates = CratesIoApi.searchCrate(key);
        List<String> variants = crates.stream().map(CratesIoApi.CrateDescription::getDependencyLine).collect(Collectors.toList());
        result.addAllElements(variants.stream().map(LookupElementBuilder::create).collect(Collectors.toList()));
    }

    @Override
    protected void completeValue(@Nonnull TomlKeyValue keyValue, @Nonnull CompletionResultSet result) {
        List<TomlKeySegment> segments = keyValue.getKey().getSegments();
        if (segments.size() != 1) return;
        TomlKeySegment key = segments.get(0);
        String version = CratesIoApi.getCrateLastVersion(key);
        if (version == null) return;

        result.addElement(
            LookupElementBuilder.create(version)
                .withInsertHandler(new Util.StringValueInsertionHandler(keyValue))
        );
    }
}

/**
 * @see org.rust.toml.CargoTomlPsiPattern#getOnSpecificDependencyHeaderKey()
 */
class CratesIoCargoTomlSpecificDependencyHeaderCompletionProvider implements CompletionProvider {
    @Override
    public void addCompletions(@Nonnull CompletionParameters parameters,
                                  @Nonnull ProcessingContext context,
                                  @Nonnull CompletionResultSet result) {
        if (!(parameters.getPosition().getParent() instanceof TomlKeySegment)) return;
        TomlKeySegment key = (TomlKeySegment) parameters.getPosition().getParent();
        Collection<CratesIoApi.CrateDescription> variants = CratesIoApi.searchCrate(key);

        for (CratesIoApi.CrateDescription variant : variants) {
            result.addElement(
                LookupElementBuilder.create(variant.name)
                    .withPresentableText(variant.getDependencyLine())
                    .withInsertHandler((ctx, item) -> {
                        TomlTable table = PsiElementExt.ancestorStrict(key, TomlTable.class);
                        if (table == null) return;
                        if (table.getEntries().isEmpty()) {
                            ctx.getDocument().insertString(
                                ctx.getSelectionEndOffset() + 1,
                                "\nversion = \"" + variant.maxVersion + "\""
                            );
                        }
                    })
            );
        }
    }
}

/**
 * @see org.rust.toml.CargoTomlPsiPattern#getInSpecificDependencyKeyValue()
 */
class CratesIoCargoTomlSpecificDependencyVersionCompletionProvider extends TomlKeyValueCompletionProviderBase {
    @Override
    protected void completeKey(@Nonnull TomlKeyValue keyValue, @Nonnull CompletionResultSet result) {
        String key;
        if (keyValue.getValue() != null) {
            key = "version";
        } else {
            TomlKeySegment dependencyNameKey = LocalCargoTomlDependencyCompletionProviders.getDependencyKey(keyValue);
            String version = CratesIoApi.getCrateLastVersion(dependencyNameKey);
            if (version == null) return;
            key = "version = \"" + version + "\"";
        }
        result.addElement(LookupElementBuilder.create(key));
    }

    @Override
    protected void completeValue(@Nonnull TomlKeyValue keyValue, @Nonnull CompletionResultSet result) {
        TomlKeySegment dependencyNameKey = LocalCargoTomlDependencyCompletionProviders.getDependencyKey(keyValue);
        String version = CratesIoApi.getCrateLastVersion(dependencyNameKey);
        if (version == null) return;

        result.addElement(
            LookupElementBuilder.create(version)
                .withInsertHandler(new Util.StringValueInsertionHandler(keyValue))
        );
    }
}
