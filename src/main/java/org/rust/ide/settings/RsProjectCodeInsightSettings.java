/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.settings;

import com.intellij.openapi.components.SimplePersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

@State(name = "RsProjectCodeInsightSettings", storages = @Storage("rust.xml"))
public class RsProjectCodeInsightSettings extends SimplePersistentStateComponent<RsProjectCodeInsightSettings.State> {

    public RsProjectCodeInsightSettings() {
        super(new State());
    }

    public static class State extends com.intellij.openapi.components.BaseState {
        private ExcludedPath[] excludedPaths = new ExcludedPath[0];

        @NotNull
        public ExcludedPath[] getExcludedPaths() {
            return excludedPaths;
        }

        public void setExcludedPaths(@NotNull ExcludedPath[] excludedPaths) {
            this.excludedPaths = excludedPaths;
            incrementModificationCount();
        }
    }

    @NotNull
    public static RsProjectCodeInsightSettings getInstance(@NotNull Project project) {
        return project.getService(RsProjectCodeInsightSettings.class);
    }
}
