/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.console;

import com.intellij.execution.console.ConsoleRootType;
import com.intellij.ide.scratch.RootType;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;

public class RsConsoleRootType extends ConsoleRootType {

    RsConsoleRootType() {
        super("rs", RsBundle.message("rust.consoles"));
    }

    @NotNull
    public static RsConsoleRootType getInstance() {
        return RootType.findByClass(RsConsoleRootType.class);
    }
}
