/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.folding;

import consulo.application.ApplicationManager;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ComponentScope;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class RsCodeFoldingSettings {

    public abstract boolean getCollapsibleOneLineMethods();
    public abstract void setCollapsibleOneLineMethods(boolean value);

    public static RsCodeFoldingSettings getInstance() {
        return ApplicationManager.getApplication().getService(RsCodeFoldingSettings.class);
    }
}
