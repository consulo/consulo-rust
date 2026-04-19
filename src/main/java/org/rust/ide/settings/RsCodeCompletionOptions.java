/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import com.intellij.application.options.CodeCompletionOptionsCustomSection;
import com.intellij.openapi.options.ConfigurableBuilder;
import org.rust.RsBundle;

/**
 * <p>
 * Configurable for Rust code completion settings.
 * and extends {@link RsCodeCompletionConfigurable} which contains the actual implementation.
 */
public class RsCodeCompletionOptions extends ConfigurableBuilder implements CodeCompletionOptionsCustomSection {

    public RsCodeCompletionOptions() {
        super(RsBundle.message("settings.rust.completion.title"));
        RsCodeInsightSettings settings = RsCodeInsightSettings.getInstance();
        checkBox(
            RsBundle.message("settings.rust.completion.suggest.out.of.scope.items"),
            () -> settings.suggestOutOfScopeItems,
            value -> settings.suggestOutOfScopeItems = value
        );
    }
}
