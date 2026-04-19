/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@Service
public final class RsPluginDisposable implements Disposable {
    @NotNull
    public static Disposable getInstance(@NotNull Project project) {
        return project.getService(RsPluginDisposable.class);
    }

    @Override
    public void dispose() {
    }
}
