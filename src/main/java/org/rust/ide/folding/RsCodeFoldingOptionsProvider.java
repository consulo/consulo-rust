/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.folding;

import com.intellij.application.options.editor.CodeFoldingOptionsProvider;
import com.intellij.openapi.options.BeanConfigurable;
import org.rust.RsBundle;

public class RsCodeFoldingOptionsProvider
    extends BeanConfigurable<RsCodeFoldingSettings>
    implements CodeFoldingOptionsProvider {

    public RsCodeFoldingOptionsProvider() {
        super(RsCodeFoldingSettings.getInstance(), RsBundle.message("settings.rust.folding.title"));
        RsCodeFoldingSettings settings = getInstance();
        if (settings != null) {
            checkBox(RsBundle.message("settings.rust.folding.one.line.methods.checkbox"),
                settings::getCollapsibleOneLineMethods, settings::setCollapsibleOneLineMethods);
        }
    }
}
