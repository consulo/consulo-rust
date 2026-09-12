/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl(id = "RsAngleBraceTypedHandler")
public class RsAngleBraceTypedHandler extends RsBraceTypedHandler {
    public RsAngleBraceTypedHandler() {
        super(AngleBraceHandler.INSTANCE);
    }
}
