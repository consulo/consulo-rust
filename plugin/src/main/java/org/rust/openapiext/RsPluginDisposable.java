/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.disposer.Disposable;
import com.intellij.openapi.components.Service;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

@Service
public final class RsPluginDisposable implements Disposable {
    @Nonnull
    public static Disposable getInstance(@Nonnull Project project) {
        return project.getService(RsPluginDisposable.class);
    }

    @Override
    public void dispose() {
    }
}
