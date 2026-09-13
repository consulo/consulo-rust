/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

/**
 * Determines cache invalidation strategy for resolve results.
 */
public enum ResolveCacheDependency {
    /**
     * Depends on the nearest RsModificationTrackerOwner and falls back to
     * rustStructureModificationTracker if the tracker owner is not found.
     */
    LOCAL,

    /**
     * Depends on rustStructureModificationTracker
     */
    RUST_STRUCTURE,

    /**
     * Depends on both LOCAL and RUST_STRUCTURE. It is not the same as "any PSI change", because,
     * for example, local changes from other functions will not invalidate the value
     */
    LOCAL_AND_RUST_STRUCTURE,

    /**
     * Depends on PsiModificationTracker.MODIFICATION_COUNT. I.e. depends on
     * any PSI change, not only in rust files
     */
    ANY_PSI_CHANGE,
}
