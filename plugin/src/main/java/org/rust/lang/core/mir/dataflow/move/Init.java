/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.WithIndex;

/**
 * Init represents a point in a program that initializes some L-value.
 */
public class Init implements WithIndex {
    private final int index;
    /** path being initialized */
    @Nonnull
    private final MovePath path;
    /** location of initialization */
    @Nonnull
    private final InitLocation location;
    /** Extra information about this initialization */
    @Nonnull
    private final InitKind kind;

    public Init(int index, @Nonnull MovePath path, @Nonnull InitLocation location, @Nonnull InitKind kind) {
        this.index = index;
        this.path = path;
        this.location = location;
        this.kind = kind;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Nonnull
    public MovePath getPath() {
        return path;
    }

    @Nonnull
    public InitLocation getLocation() {
        return location;
    }

    @Nonnull
    public InitKind getKind() {
        return kind;
    }
}
