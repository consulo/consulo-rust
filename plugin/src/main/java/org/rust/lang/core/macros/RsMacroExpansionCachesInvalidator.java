/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.CachesInvalidator;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public class RsMacroExpansionCachesInvalidator extends CachesInvalidator {
    @Override
    public consulo.localize.LocalizeValue getDescription() {
        return consulo.localize.LocalizeValue.of("Rust macro expansion caches");
    }
    @Override
    public void invalidateCaches() {
        try {
            MacroExpansionManager.invalidateCaches();
        } catch (Exception e) {
            MacroExpansionManagerUtil.MACRO_LOG.warn(e);
        }
    }
}
