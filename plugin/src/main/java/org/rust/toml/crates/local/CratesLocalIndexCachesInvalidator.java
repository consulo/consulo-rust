/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.CachesInvalidator;
import consulo.logging.Logger;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public class CratesLocalIndexCachesInvalidator extends CachesInvalidator {
    private static final Logger LOG = Logger.getInstance(CratesLocalIndexCachesInvalidator.class);

    @Override
    public consulo.localize.LocalizeValue getDescription() {
        return consulo.localize.LocalizeValue.of("Crates.io local index");
    }

    @Override
    public void invalidateCaches() {
        try {
            CratesLocalIndexServiceImpl.invalidateCaches();
        } catch (Exception e) {
            LOG.warn(e);
        }
    }
}
