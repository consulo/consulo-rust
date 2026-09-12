/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl(id = "RsAngleBraceBackspaceHandler")
public class RsAngleBraceBackspaceHandler extends RsBraceBackspaceHandler {
    public RsAngleBraceBackspaceHandler() {
        super(AngleBraceHandler.INSTANCE);
    }
}
