/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.openapiext.PsiElementExtUtil;
import org.rust.toml.Util;
import org.rust.toml.resolve.CargoTomlNameResolution;
import org.toml.lang.psi.TomlArray;
import org.toml.lang.psi.TomlKeySegment;
import org.toml.lang.psi.TomlFile;

import java.util.HashSet;
import java.util.Set;

/**
 * Provides completion for dependency features in Cargo.toml
 */
public class CargoTomlDependencyFeaturesCompletionProvider extends CompletionProvider<CompletionParameters> {

    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                  @NotNull ProcessingContext context,
                                  @NotNull CompletionResultSet result) {
        TomlArray containingArray = PsiElementExtUtil.ancestorOrSelf(parameters.getPosition(), TomlArray.class);
        if (containingArray == null) return;
        TomlKeySegment depKey = Util.getContainingDependencyKey(containingArray);
        if (depKey == null) return;
        String pkgName = depKey.getText();

        TomlFile depToml = Util.findDependencyTomlFile(containingArray, pkgName);
        if (depToml == null) return;
        TomlArray originalArray = (TomlArray) CompletionUtil.getOriginalElement(containingArray);
        if (originalArray == null) return;

        Set<String> presentFeatures = new HashSet<>();
        for (com.intellij.psi.PsiElement el : originalArray.getElements()) {
            String sv = Util.getStringValue((org.toml.lang.psi.TomlValue) el);
            if (sv != null) presentFeatures.add(sv);
        }

        for (TomlKeySegment feature : CargoTomlNameResolution.allFeatures(depToml, false)) {
            if (feature.getName() != null && !presentFeatures.contains(feature.getName())) {
                result.addElement(CargoTomlLookupElements.lookupElementForFeature(feature));
            }
        }
    }
}
