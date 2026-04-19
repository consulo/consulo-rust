/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.snapshot;

import org.jetbrains.annotations.NotNull;

public class CombinedSnapshot implements Snapshot {
    @NotNull
    private final Snapshot[] mySnapshots;

    public CombinedSnapshot(@NotNull Snapshot... snapshots) {
        mySnapshots = snapshots;
    }

    @Override
    public void rollback() {
        for (Snapshot snapshot : mySnapshots) {
            snapshot.rollback();
        }
    }

    @Override
    public void commit() {
        for (Snapshot snapshot : mySnapshots) {
            snapshot.commit();
        }
    }
}
