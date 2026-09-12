/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject.state;

import consulo.application.ApplicationManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;

@State(name = "RsUserTemplatesState", storages = @Storage("rust.usertemplates"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class RsUserTemplatesState implements PersistentStateComponent<RsUserTemplatesState> {

    public List<RsUserTemplate> templates = new ArrayList<>();

    @Nonnull
    @Override
    public RsUserTemplatesState getState() {
        return this;
    }

    @Override
    public void loadState(@Nonnull RsUserTemplatesState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    @Nonnull
    public static RsUserTemplatesState getInstance() {
        return ApplicationManager.getApplication().getService(RsUserTemplatesState.class);
    }
}
