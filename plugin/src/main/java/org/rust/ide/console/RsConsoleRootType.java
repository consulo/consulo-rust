/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.ui.console.ConsoleRootType;
import consulo.language.scratch.RootType;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;

@ExtensionImpl
public class RsConsoleRootType extends ConsoleRootType {

    public RsConsoleRootType() {
        super("rs", RsBundle.message("rust.consoles"));
    }

    @Nonnull
    public static RsConsoleRootType getInstance() {
        return RootType.findByClass(RsConsoleRootType.class);
    }
}
