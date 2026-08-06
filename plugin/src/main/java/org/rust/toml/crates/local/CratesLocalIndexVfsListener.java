/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import consulo.virtualFileSystem.event.AsyncFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public class CratesLocalIndexVfsListener implements AsyncFileListener {

    @Nullable
    @Override
    public ChangeApplier prepareChange(@Nonnull List<? extends VFileEvent> events) {
        CratesLocalIndexService service = CratesLocalIndexService.getInstanceIfCreated();
        if (!(service instanceof CratesLocalIndexServiceImpl)) return null;
        CratesLocalIndexServiceImpl impl = (CratesLocalIndexServiceImpl) service;

        if (!impl.hasInterestingEvent(events)) return null;

        return new ChangeApplier() {
            @Override
            public void afterVfsChange() {
                impl.updateIndex();
            }
        };
    }
}
