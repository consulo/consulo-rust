/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsModDeclItem;

public class RsModDeclUsageInfo extends RsMoveUsageInfo {

    @NotNull
    private final RsModDeclItem element;
    @NotNull
    private final RsFile file;

    public RsModDeclUsageInfo(@NotNull RsModDeclItem element, @NotNull RsFile file) {
        super(element);
        this.element = element;
        this.file = file;
    }

    @NotNull
    public RsModDeclItem getElement() {
        return element;
    }

    @NotNull
    public RsFile getFile() {
        return file;
    }
}
