/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.crate;

/**
 * Persistent {@link Crate} identifier. Guaranteed to be positive 32-bit integer.
 * This id can be saved to a disk and then used to find the crate
 *
 * See {@link Crate#getId()}
 *
 * See {@link CrateGraphService#findCrateById(int)}
 *
 */
public final class CratePersistentId {
    private CratePersistentId() {
        // This class is not meant to be instantiated.
        // In Java, use int or Integer directly wherever CratePersistentId was used.
    }
}
