/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CratesLocalIndexVfsListener implements AsyncFileListener {

    @Nullable
    @Override
    public ChangeApplier prepareChange(@NotNull List<? extends VFileEvent> events) {
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
