/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import org.rust.settings.RsVcsConfigurationState;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@State(name = "RsVcsConfiguration", storages = @Storage("rust"))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class RsVcsConfiguration implements PersistentStateComponent<RsVcsConfigurationState> {
    @Nonnull
    public static RsVcsConfiguration getInstance(@Nonnull Project project) {
        return project.getInstance(RsVcsConfiguration.class);
    }

    private RsVcsConfigurationState myState = new RsVcsConfigurationState();

    @Nullable
    @Override
    public RsVcsConfigurationState getState() {
        return myState;
    }

    @Override
    public void loadState(RsVcsConfigurationState state) {
        myState = state;
    }
}
