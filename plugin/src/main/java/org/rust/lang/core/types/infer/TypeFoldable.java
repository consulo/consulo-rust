/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;

/**
 * Despite a scary name, TypeFoldable is a rather simple thing.
 *
 * It allows to map type variables within a type (or another object,
 * containing a type, like a Predicate) to other types.
 */
public interface TypeFoldable<Self> {
    /**
     * Fold this type with the folder.
     */
    @Nonnull
    default Self foldWith(@Nonnull TypeFolder folder) {
        return superFoldWith(folder);
    }

    /**
     * Fold inner types (not this type) with the folder.
     */
    @Nonnull
    Self superFoldWith(@Nonnull TypeFolder folder);

    /** Similar to superVisitWith, but just visit types without folding */
    default boolean visitWith(@Nonnull TypeVisitor visitor) {
        return superVisitWith(visitor);
    }

    /** Similar to foldWith, but just visit types without folding */
    boolean superVisitWith(@Nonnull TypeVisitor visitor);
}
