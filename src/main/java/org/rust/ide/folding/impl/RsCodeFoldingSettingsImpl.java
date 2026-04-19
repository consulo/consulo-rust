/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.folding.impl;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.folding.RsCodeFoldingSettings;

@State(name = "RsCodeFoldingSettings", storages = @Storage("editor.codeinsight.xml"))
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
    public @NotNull RsCodeFoldingSettingsImpl getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull RsCodeFoldingSettingsImpl state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
