/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import com.intellij.openapi.components.SimplePersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;
import org.rust.settings.ExcludedPath;

@State(name = "RsProjectCodeInsightSettings", storages = @Storage("rust"))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class RsProjectCodeInsightSettings extends SimplePersistentStateComponent<RsProjectCodeInsightSettings.State> {

    public RsProjectCodeInsightSettings() {
        super(new State());
    }

    public static class State extends com.intellij.openapi.components.BaseState {
        private ExcludedPath[] excludedPaths = new ExcludedPath[0];

        @Nonnull
        public ExcludedPath[] getExcludedPaths() {
            return excludedPaths;
        }

        public void setExcludedPaths(@Nonnull ExcludedPath[] excludedPaths) {
            this.excludedPaths = excludedPaths;
            incrementModificationCount();
        }
    }

    @Nonnull
    public static RsProjectCodeInsightSettings getInstance(@Nonnull Project project) {
        return project.getService(RsProjectCodeInsightSettings.class);
    }
}
