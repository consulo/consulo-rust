/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.folding.impl;

import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializerUtil;
import jakarta.annotation.Nonnull;
import org.rust.ide.folding.RsCodeFoldingSettings;
import consulo.annotation.component.ServiceImpl;

@State(name = "RsCodeFoldingSettings", storages = @Storage("editor.codeinsight"))
@ServiceImpl
public class RsCodeFoldingSettingsImpl extends RsCodeFoldingSettings implements PersistentStateComponent<RsCodeFoldingSettingsImpl> {

    private boolean collapsibleOneLineMethods = true;

    @Override
    public boolean getCollapsibleOneLineMethods() {
        return collapsibleOneLineMethods;
    }

    @Override
    public void setCollapsibleOneLineMethods(boolean value) {
        this.collapsibleOneLineMethods = value;
    }

    @Override
    public @Nonnull RsCodeFoldingSettingsImpl getState() {
        return this;
    }

    @Override
    public void loadState(@Nonnull RsCodeFoldingSettingsImpl state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
