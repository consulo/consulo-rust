/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.snapshot;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ListIterator;

class LogBasedSnapshot implements Snapshot {
    @NotNull
    private final List<Undoable> myUndoLog;
    private final int myPosition;

    private LogBasedSnapshot(@NotNull List<Undoable> undoLog, int position) {
        myUndoLog = undoLog;
        myPosition = position;
    }

    @Override
    public void commit() {
        assertOpenSnapshot();
        if (myPosition == 0) {
            myUndoLog.clear();
        } else {
            myUndoLog.set(myPosition, COMMITTED_SNAPSHOT);
        }
    }

    @Override
    public void rollback() {
        assertOpenSnapshot();
        if (myPosition + 1 != myUndoLog.size()) {
            List<Undoable> toRollback = myUndoLog.subList(myPosition + 1, myUndoLog.size());
            // Iterate in reverse
            ListIterator<Undoable> iter = toRollback.listIterator(toRollback.size());
            while (iter.hasPrevious()) {
                iter.previous().undo();
            }
            toRollback.clear();
        }

        Undoable last = myUndoLog.remove(myUndoLog.size() - 1);
        if (last != OPEN_SNAPSHOT) {
            throw new IllegalStateException("Expected OpenSnapshot");
        }
        if (myUndoLog.size() != myPosition) {
            throw new IllegalStateException("Expected undo log size == position");
        }
    }

    private void assertOpenSnapshot() {
        if (myPosition >= myUndoLog.size() || myUndoLog.get(myPosition) != OPEN_SNAPSHOT) {
            throw new IllegalStateException("Expected OpenSnapshot at position " + myPosition);
        }
    }

    @NotNull
    static Snapshot start(@NotNull List<Undoable> undoLog) {
        undoLog.add(OPEN_SNAPSHOT);
        return new LogBasedSnapshot(undoLog, undoLog.size() - 1);
    }

    private static final Undoable OPEN_SNAPSHOT = () -> {
        throw new IllegalStateException("Cannot rollback an uncommitted snapshot");
    };

    private static final Undoable COMMITTED_SNAPSHOT = () -> {
        // This occurs when there are nested snapshots and
        // the inner is committed but outer is rolled back.
    };
}
