/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.application.CachesInvalidator;

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
