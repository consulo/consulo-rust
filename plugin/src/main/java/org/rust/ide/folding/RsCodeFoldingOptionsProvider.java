/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.folding;

import com.intellij.codeInsight.folding.CodeFoldingOptionsProvider;
import consulo.configurable.BeanConfigurable;

public class RsCodeFoldingOptionsProvider
    extends BeanConfigurable<RsCodeFoldingSettings>
    implements CodeFoldingOptionsProvider {

    public RsCodeFoldingOptionsProvider() {
        super(RsCodeFoldingSettings.getInstance());
        // TODO: wire `settings.rust.folding.one.line.methods.checkbox` via Consulo's fluent configurable API
    }
}
