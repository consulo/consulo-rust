/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@State(name = "RsUserTemplatesState", storages = @Storage("rust.usertemplates.xml"))
public class RsUserTemplatesState implements PersistentStateComponent<RsUserTemplatesState> {

    public List<RsUserTemplate> templates = new ArrayList<>();

    @NotNull
    @Override
    public RsUserTemplatesState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull RsUserTemplatesState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @NotNull
    public static RsUserTemplatesState getInstance() {
        return ApplicationManager.getApplication().getService(RsUserTemplatesState.class);
    }
}
