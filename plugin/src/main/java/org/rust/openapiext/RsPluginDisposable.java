/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.disposer.Disposable;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class RsPluginDisposable implements Disposable {
    @Nonnull
    public static Disposable getInstance(@Nonnull Project project) {
        return project.getService(RsPluginDisposable.class);
    }

    @Override
    public void dispose() {
    }
}
