/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.schemas.MirProjectionElem;
import org.rust.lang.core.mir.schemas.MirLocal;
import org.rust.lang.core.mir.schemas.MirPlace;
import org.rust.lang.core.mir.schemas.PlaceElem;

import java.util.Map;

/** Tables mapping from a MirPlace to its MovePath */
public class MovePathLookup {
    @NotNull
    private final Map<MirLocal, MovePath> locals;
    @NotNull
    private final Map<Pair<MovePath, MirProjectionElem<?>>, MovePath> projections;

    public MovePathLookup(@NotNull Map<MirLocal, MovePath> locals, @NotNull Map<Pair<MovePath, MirProjectionElem<?>>, MovePath> projections) {
        this.locals = locals;
        this.projections = projections;
    }

    @NotNull
    public LookupResult find(@NotNull MirPlace place) {
        MovePath result = locals.get(place.getLocal());
        if (result == null) {
            throw new IllegalStateException("Local not found in MovePathLookup: " + place.getLocal());
        }
        for (PlaceElem elem : place.getProjections()) {
            Pair<MovePath, MirProjectionElem<?>> key = new Pair<>(result, elem.lift());
            MovePath next = projections.get(key);
            if (next == null) {
                return new LookupResult.Parent(result);
            }
            result = next;
        }
        return new LookupResult.Exact(result);
    }

    @NotNull
    public MovePath find(@NotNull MirLocal local) {
        MovePath result = locals.get(local);
        if (result == null) {
            throw new IllegalStateException("Local not found in MovePathLookup: " + local);
        }
        return result;
    }

    @NotNull
    public Map<MirLocal, MovePath> getLocals() {
        return locals;
    }

    @NotNull
    public Map<Pair<MovePath, MirProjectionElem<?>>, MovePath> getProjections() {
        return projections;
    }
}
