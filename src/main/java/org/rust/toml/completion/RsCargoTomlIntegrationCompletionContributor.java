/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import org.rust.toml.Util;

/**
 * Provides completion in Rust files for elements that point to TOML elements, e.g. for cargo features
 */
public class RsCargoTomlIntegrationCompletionContributor extends CompletionContributor {
    public RsCargoTomlIntegrationCompletionContributor() {
        if (Util.tomlPluginIsAbiCompatible()) {
            extend(CompletionType.BASIC, RsCfgFeatureCompletionProvider.INSTANCE.getElementPattern(), RsCfgFeatureCompletionProvider.INSTANCE);
        }
    }
}
