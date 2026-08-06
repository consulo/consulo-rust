/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.snapshot;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

public class UndoLog {
    @Nonnull
    private final List<Undoable> myUndoLog = new ArrayList<>();

    public void logChange(@Nonnull Undoable undoable) {
        if (inSnapshot()) {
            myUndoLog.add(undoable);
        }
    }

    @Nonnull
    public Snapshot startSnapshot() {
        return LogBasedSnapshot.start(myUndoLog);
    }

    private boolean inSnapshot() {
        return !myUndoLog.isEmpty();
    }

    @Nonnull
    List<Undoable> getUndoLog() {
        return myUndoLog;
    }
}
