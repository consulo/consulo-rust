/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.dataflow.impls.BorrowsUtil;
import org.rust.lang.core.mir.dataflow.move.MoveData;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.mir.util.IndexAlloc;
import org.rust.lang.core.mir.util.IndexKeyMap;

import java.util.*;

public class BorrowSet implements Iterable<BorrowData> {
    /**
     * The fundamental map relating bitvector indexes to the borrows in the MIR. Each borrow is also uniquely identified
     * in the MIR by the MirLocation of the assignment statement in which it appears on the right hand side. Thus the
     * location is the map key, and its position in the map corresponds to BorrowData.
     */
    @NotNull
    private final Map<MirLocation, BorrowData> locationMap;

    /**
     * Locations which activate borrows.
     * NOTE: a given location may activate more than one borrow in the future when more general two-phase borrow support
     * is introduced, but for now we only need to store one borrow index.
     */
    @NotNull
    private final Map<MirLocation, List<BorrowData>> activationMap;

    /** Map from local to all the borrows on that local. */
    @NotNull
    private final IndexKeyMap<MirLocal, Set<BorrowData>> localMap;

    @NotNull
    private final LocalsStateAtExit localsStateAtExit;

    @NotNull
    private final IndexAlloc<BorrowData> borrowData;

    private BorrowSet(
        @NotNull Map<MirLocation, BorrowData> locationMap,
        @NotNull Map<MirLocation, List<BorrowData>> activationMap,
        @NotNull IndexKeyMap<MirLocal, Set<BorrowData>> localMap,
        @NotNull LocalsStateAtExit localsStateAtExit,
        @NotNull IndexAlloc<BorrowData> borrowData
    ) {
        this.locationMap = locationMap;
        this.activationMap = activationMap;
        this.localMap = localMap;
        this.localsStateAtExit = localsStateAtExit;
        this.borrowData = borrowData;
    }

    public int getSize() {
        return borrowData.getSize();
    }

    @NotNull
    public Map<MirLocation, BorrowData> getLocationMap() {
        return locationMap;
    }

    @NotNull
    public IndexKeyMap<MirLocal, Set<BorrowData>> getLocalMap() {
        return localMap;
    }

    @NotNull
    public LocalsStateAtExit getLocalsStateAtExit() {
        return localsStateAtExit;
    }

    @Override
    @NotNull
    public Iterator<BorrowData> iterator() {
        return locationMap.values().iterator();
    }

    @NotNull
    public List<BorrowData> activationsAtLocation(@NotNull MirLocation location) {
        List<BorrowData> result = activationMap.get(location);
        return result != null ? result : Collections.emptyList();
    }

    @NotNull
    public static BorrowSet build(
        @NotNull MirBody body,
        boolean localsAreInvalidatedAtExit,
        @NotNull MoveData moveData
    ) {
        GatherBorrows visitor = new GatherBorrows(
            body,
            new HashMap<>(),
            new HashMap<>(),
            new IndexKeyMap<>(),
            new IndexKeyMap<>(),
            LocalsStateAtExit.build(localsAreInvalidatedAtExit, body, moveData),
            new IndexAlloc<>()
        );

        for (MirBasicBlock block : Utils.getBasicBlocksInPreOrder(body)) {
            visitor.visitBasicBlock(block);
        }

        return new BorrowSet(
            visitor.getLocationMap(),
            visitor.getActivationMap(),
            visitor.getLocalMap(),
            visitor.getLocalsStateAtExit(),
            visitor.getBorrowData()
        );
    }
}
