/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.WithIndex;

/**
 * Init represents a point in a program that initializes some L-value.
 */
public class Init implements WithIndex {
    private final int index;
    /** path being initialized */
    @NotNull
    private final MovePath path;
    /** location of initialization */
    @NotNull
    private final InitLocation location;
    /** Extra information about this initialization */
    @NotNull
    private final InitKind kind;

    public Init(int index, @NotNull MovePath path, @NotNull InitLocation location, @NotNull InitKind kind) {
        this.index = index;
        this.path = path;
        this.location = location;
        this.kind = kind;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @NotNull
    public MovePath getPath() {
        return path;
    }

    @NotNull
    public InitLocation getLocation() {
        return location;
    }

    @NotNull
    public InitKind getKind() {
        return kind;
    }
}
