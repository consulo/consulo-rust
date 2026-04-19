/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.snapshot;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UndoLog {
    @NotNull
    private final List<Undoable> myUndoLog = new ArrayList<>();

    public void logChange(@NotNull Undoable undoable) {
        if (inSnapshot()) {
            myUndoLog.add(undoable);
        }
    }

    @NotNull
    public Snapshot startSnapshot() {
        return LogBasedSnapshot.start(myUndoLog);
    }

    private boolean inSnapshot() {
        return !myUndoLog.isEmpty();
    }

    @NotNull
    List<Undoable> getUndoLog() {
        return myUndoLog;
    }
}
