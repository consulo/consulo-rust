/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.snapshot;

import jakarta.annotation.Nonnull;

public class CombinedSnapshot implements Snapshot {
    @Nonnull
    private final Snapshot[] mySnapshots;

    public CombinedSnapshot(@Nonnull Snapshot... snapshots) {
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
