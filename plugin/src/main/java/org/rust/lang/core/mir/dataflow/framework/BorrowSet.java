/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
    @Nonnull
    private final Map<MirLocation, BorrowData> locationMap;

    /**
     * Locations which activate borrows.
     * NOTE: a given location may activate more than one borrow in the future when more general two-phase borrow support
     * is introduced, but for now we only need to store one borrow index.
     */
    @Nonnull
    private final Map<MirLocation, List<BorrowData>> activationMap;

    /** Map from local to all the borrows on that local. */
    @Nonnull
    private final IndexKeyMap<MirLocal, Set<BorrowData>> localMap;

    @Nonnull
    private final LocalsStateAtExit localsStateAtExit;

    @Nonnull
    private final IndexAlloc<BorrowData> borrowData;

    private BorrowSet(
        @Nonnull Map<MirLocation, BorrowData> locationMap,
        @Nonnull Map<MirLocation, List<BorrowData>> activationMap,
        @Nonnull IndexKeyMap<MirLocal, Set<BorrowData>> localMap,
        @Nonnull LocalsStateAtExit localsStateAtExit,
        @Nonnull IndexAlloc<BorrowData> borrowData
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

    @Nonnull
    public Map<MirLocation, BorrowData> getLocationMap() {
        return locationMap;
    }

    @Nonnull
    public IndexKeyMap<MirLocal, Set<BorrowData>> getLocalMap() {
        return localMap;
    }

    @Nonnull
    public LocalsStateAtExit getLocalsStateAtExit() {
        return localsStateAtExit;
    }

    @Override
    @Nonnull
    public Iterator<BorrowData> iterator() {
        return locationMap.values().iterator();
    }

    @Nonnull
    public List<BorrowData> activationsAtLocation(@Nonnull MirLocation location) {
        List<BorrowData> result = activationMap.get(location);
        return result != null ? result : Collections.emptyList();
    }

    @Nonnull
    public static BorrowSet build(
        @Nonnull MirBody body,
        boolean localsAreInvalidatedAtExit,
        @Nonnull MoveData moveData
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
