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

@State(name = "RsVcsConfiguration", storages = @Storage("rust.xml"))
public class RsVcsConfiguration extends SimplePersistentStateComponent<RsVcsConfiguration.State> {

    public RsVcsConfiguration() {
        super(new State());
    }

    public static class State extends com.intellij.openapi.components.BaseState {
        private boolean rustFmt = false;

        public boolean getRustFmt() {
            return rustFmt;
        }

        public void setRustFmt(boolean value) {
            this.rustFmt = value;
            incrementModificationCount();
        }
    }

    @NotNull
    public static RsVcsConfiguration getInstance(@NotNull Project project) {
        return project.getService(RsVcsConfiguration.class);
    }
}
