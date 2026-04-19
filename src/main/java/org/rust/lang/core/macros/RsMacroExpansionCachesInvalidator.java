/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.ide.caches.CachesInvalidator;

public class RsMacroExpansionCachesInvalidator extends CachesInvalidator {
    @Override
    public void invalidateCaches() {
        try {
            MacroExpansionManager.invalidateCaches();
        } catch (Exception e) {
            MacroExpansionManagerUtil.MACRO_LOG.warn(e);
        }
    }
}
