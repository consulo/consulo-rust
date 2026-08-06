/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.snapshot;

import jakarta.annotation.Nonnull;

/**
 * An entity that allows you to take a snapshot ({@link #startSnapshot()}) and then roll back to snapshot state.
 */
public abstract class Snapshotable {
    @Nonnull
    protected final UndoLog myUndoLog = new UndoLog();

    @Nonnull
    public Snapshot startSnapshot() {
        return myUndoLog.startSnapshot();
    }
}
