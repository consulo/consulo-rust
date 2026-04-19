/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import com.intellij.application.options.CodeCompletionOptionsCustomSection;
import com.intellij.openapi.options.ConfigurableBuilder;
import org.rust.RsBundle;

public class RsCodeCompletionConfigurable extends ConfigurableBuilder implements CodeCompletionOptionsCustomSection {

    public RsCodeCompletionConfigurable() {
        super(RsBundle.message("settings.rust.completion.title"));
        RsCodeInsightSettings settings = RsCodeInsightSettings.getInstance();
        checkBox(
            RsBundle.message("settings.rust.completion.suggest.out.of.scope.items"),
            () -> settings.suggestOutOfScopeItems,
            value -> settings.suggestOutOfScopeItems = value
        );
    }
}
