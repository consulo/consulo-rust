/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import com.intellij.ide.caches.CachesInvalidator;
import com.intellij.openapi.diagnostic.Logger;

public class CratesLocalIndexCachesInvalidator extends CachesInvalidator {
    private static final Logger LOG = Logger.getInstance(CratesLocalIndexCachesInvalidator.class);

    @Override
    public void invalidateCaches() {
        try {
            CratesLocalIndexServiceImpl.invalidateCaches();
        } catch (Exception e) {
            LOG.warn(e);
        }
    }
}
