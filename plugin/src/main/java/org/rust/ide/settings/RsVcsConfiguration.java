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

@State(name = "RsVcsConfiguration", storages = @Storage("rust"))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
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

    @Nonnull
    public static RsVcsConfiguration getInstance(@Nonnull Project project) {
        return project.getService(RsVcsConfiguration.class);
    }
}
