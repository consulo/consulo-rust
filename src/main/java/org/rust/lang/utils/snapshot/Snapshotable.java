/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.snapshot;

import org.jetbrains.annotations.NotNull;

/**
 * An entity that allows you to take a snapshot ({@link #startSnapshot()}) and then roll back to snapshot state.
 */
public abstract class Snapshotable {
    @NotNull
    protected final UndoLog myUndoLog = new UndoLog();

    @NotNull
    public Snapshot startSnapshot() {
        return myUndoLog.startSnapshot();
    }
}
