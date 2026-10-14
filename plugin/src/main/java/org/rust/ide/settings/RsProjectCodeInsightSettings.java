/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import org.rust.settings.RsProjectCodeInsightSettingsState;
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

@State(name = "RsProjectCodeInsightSettings", storages = @Storage("rust"))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class RsProjectCodeInsightSettings implements PersistentStateComponent<RsProjectCodeInsightSettingsState> {
    @Nonnull
    public static RsProjectCodeInsightSettings getInstance(@Nonnull Project project) {
        return project.getInstance(RsProjectCodeInsightSettings.class);
    }

    private RsProjectCodeInsightSettingsState myState = new RsProjectCodeInsightSettingsState();

    @Nullable
    @Override
    public RsProjectCodeInsightSettingsState getState() {
        return myState;
    }

    @Override
    public void loadState(RsProjectCodeInsightSettingsState state) {
        myState = state;
    }
}
