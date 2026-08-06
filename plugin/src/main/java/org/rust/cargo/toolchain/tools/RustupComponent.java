/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import org.rust.cargo.toolchain.RsToolchainBase;

public abstract class RustupComponent extends RsTool {
    public RustupComponent(String componentName, RsToolchainBase toolchain) {
        super(componentName, toolchain);
    }
}
