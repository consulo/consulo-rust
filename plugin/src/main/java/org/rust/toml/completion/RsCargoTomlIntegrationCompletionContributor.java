/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.completion;
import org.rust.lang.RsLanguage;
import consulo.language.Language;

import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionType;
import org.rust.toml.Util;

/**
 * Provides completion in Rust files for elements that point to TOML elements, e.g. for cargo features
 */
public class RsCargoTomlIntegrationCompletionContributor extends CompletionContributor {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.toml.lang.TomlLanguage.INSTANCE; }

    public RsCargoTomlIntegrationCompletionContributor() {
        if (Util.tomlPluginIsAbiCompatible()) {
            extend(CompletionType.BASIC, RsCfgFeatureCompletionProvider.INSTANCE.getElementPattern(), RsCfgFeatureCompletionProvider.INSTANCE);
        }
    }
}
