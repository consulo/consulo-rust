/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;

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
    @NotNull
    default Self foldWith(@NotNull TypeFolder folder) {
        return superFoldWith(folder);
    }

    /**
     * Fold inner types (not this type) with the folder.
     */
    @NotNull
    Self superFoldWith(@NotNull TypeFolder folder);

    /** Similar to superVisitWith, but just visit types without folding */
    default boolean visitWith(@NotNull TypeVisitor visitor) {
        return superVisitWith(visitor);
    }

    /** Similar to foldWith, but just visit types without folding */
    boolean superVisitWith(@NotNull TypeVisitor visitor);
}
