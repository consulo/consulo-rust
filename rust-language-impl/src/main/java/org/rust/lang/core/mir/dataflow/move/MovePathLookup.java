/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.schemas.MirProjectionElem;
import org.rust.lang.core.mir.schemas.MirLocal;
import org.rust.lang.core.mir.schemas.MirPlace;
import org.rust.lang.core.mir.schemas.PlaceElem;

import java.util.Map;

/** Tables mapping from a MirPlace to its MovePath */
public class MovePathLookup {
    @Nonnull
    private final Map<MirLocal, MovePath> locals;
    @Nonnull
    private final Map<Pair<MovePath, MirProjectionElem<?>>, MovePath> projections;

    public MovePathLookup(@Nonnull Map<MirLocal, MovePath> locals, @Nonnull Map<Pair<MovePath, MirProjectionElem<?>>, MovePath> projections) {
        this.locals = locals;
        this.projections = projections;
    }

    @Nonnull
    public LookupResult find(@Nonnull MirPlace place) {
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

    @Nonnull
    public MovePath find(@Nonnull MirLocal local) {
        MovePath result = locals.get(local);
        if (result == null) {
            throw new IllegalStateException("Local not found in MovePathLookup: " + local);
        }
        return result;
    }

    @Nonnull
    public Map<MirLocal, MovePath> getLocals() {
        return locals;
    }

    @Nonnull
    public Map<Pair<MovePath, MirProjectionElem<?>>, MovePath> getProjections() {
        return projections;
    }
}
