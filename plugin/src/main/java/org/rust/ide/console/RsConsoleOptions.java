/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import com.intellij.openapi.components.Service;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@State(name = "RsConsoleOptions", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class RsConsoleOptions implements PersistentStateComponent<RsConsoleOptions> {

    public boolean showVariables = true;

    @Override
    @Nonnull
    public RsConsoleOptions getState() {
        return this;
    }

    @Override
    public void loadState(@Nonnull RsConsoleOptions state) {
        showVariables = state.showVariables;
    }

    @Nonnull
    public static RsConsoleOptions getInstance(@Nonnull Project project) {
        return project.getService(RsConsoleOptions.class);
    }
}
