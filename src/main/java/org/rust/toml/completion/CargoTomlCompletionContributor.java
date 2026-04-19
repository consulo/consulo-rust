/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import org.rust.toml.CargoTomlPsiPattern;
import org.rust.toml.Util;

import java.util.List;

public class CargoTomlCompletionContributor extends CompletionContributor {

    private static final List<String> POPULAR_SPDX_LICENSES = List.of(
        "AGPL-3", "Apache-2.0", "BSD-2", "BSD-3", "BSL-1", "CC0-1",
        "EPL-2", "GPL-2", "GPL-3", "LGPL-2", "MIT", "MPL-2"
    );

    public CargoTomlCompletionContributor() {
        if (Util.tomlPluginIsAbiCompatible()) {
            CargoTomlPsiPattern pattern = CargoTomlPsiPattern.INSTANCE;
            extend(CompletionType.BASIC, pattern.getInKey(), new CargoTomlKeysCompletionProvider());
            extend(CompletionType.BASIC, pattern.inValueWithKey("edition"),
                new CargoTomlKnownValuesCompletionProvider(List.of("2015", "2018", "2021")));
            extend(CompletionType.BASIC, pattern.inValueWithKey("license"),
                new CargoTomlKnownValuesCompletionProvider(POPULAR_SPDX_LICENSES));
            extend(CompletionType.BASIC, pattern.getInFeatureDependencyArray(),
                new CargoTomlFeatureDependencyCompletionProvider());
            extend(CompletionType.BASIC, pattern.getInDependencyPackageFeatureArray(),
                new CargoTomlDependencyFeaturesCompletionProvider());
            extend(CompletionType.BASIC, pattern.getInDependencyTableKey(),
                new CargoTomlDependencyKeysCompletionProvider());

            // Available using both Crates.io API & Crates Local Index
            extend(CompletionType.BASIC, pattern.getInDependencyKeyValue(),
                new CargoTomlDependencyCompletionProvider());
            extend(CompletionType.BASIC, pattern.getInSpecificDependencyHeaderKey(),
                new CargoTomlSpecificDependencyHeaderCompletionProvider());
            extend(CompletionType.BASIC, pattern.getInSpecificDependencyKeyValue(),
                new CargoTomlSpecificDependencyVersionCompletionProvider());

            // Available only using Crates Local Index
            extend(CompletionType.BASIC, pattern.getInDependencyInlineTableVersion(),
                new LocalCargoTomlInlineTableVersionCompletionProvider());
        }
    }
}
