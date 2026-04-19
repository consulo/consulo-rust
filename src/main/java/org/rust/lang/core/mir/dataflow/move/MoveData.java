/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.*;

import java.util.List;
import java.util.Map;

public interface MoveData {
    /**
     * All MoveOut's grouped by MoveOut.source.
     * (There can be multiple MoveOut's for a given MirLocation)
     */
    @NotNull
    Map<MirLocation, List<MoveOut>> getLocMap();

    /** All MoveOut's grouped by MoveOut.path. */
    @NotNull
    Map<MovePath, List<MoveOut>> getPathMap();

    /** Maps MirPlace to the nearest MovePath */
    @NotNull
    MovePathLookup getRevLookup();

    /** Init's grouped by Init.location when the location is InitLocation.Statement */
    @NotNull
    Map<MirLocation, List<Init>> getInitLocMap();

    /** All Init's grouped by Init.path */
    @NotNull
    Map<MovePath, List<Init>> getInitPathMap();

    /** The number of MovePath's exists in this MoveData */
    int getMovePathsCount();

    /**
     * For the move path initPath, returns the root local variable (if any) that starts the path. (e.g., for a path
     * like a.b.c returns Some(a))
     */
    @Nullable
    default MirLocal baseLocal(@NotNull MovePath initPath) {
        MovePath path = initPath;
        while (true) {
            MirLocal local = path.getPlace().getLocal();
            if (local != null) return local;
            MovePath parent = path.getParent();
            if (parent != null) {
                path = parent;
            } else {
                return null;
            }
        }
    }

    @NotNull
    static MoveData gatherMoves(@NotNull MirBody body) {
        return MoveDataBuilder.gatherMoves(body);
    }
}
