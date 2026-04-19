/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.folding;

import com.intellij.openapi.application.ApplicationManager;

public abstract class RsCodeFoldingSettings {

    public abstract boolean getCollapsibleOneLineMethods();
    public abstract void setCollapsibleOneLineMethods(boolean value);

    public static RsCodeFoldingSettings getInstance() {
        return ApplicationManager.getApplication().getService(RsCodeFoldingSettings.class);
    }
}
