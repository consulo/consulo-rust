/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(name = "RsConsoleOptions", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class RsConsoleOptions implements PersistentStateComponent<RsConsoleOptions> {

    public boolean showVariables = true;

    @Override
    @NotNull
    public RsConsoleOptions getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull RsConsoleOptions state) {
        showVariables = state.showVariables;
    }

    @NotNull
    public static RsConsoleOptions getInstance(@NotNull Project project) {
        return project.getService(RsConsoleOptions.class);
    }
}
