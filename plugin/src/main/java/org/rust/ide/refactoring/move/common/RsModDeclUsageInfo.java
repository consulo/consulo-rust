/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.RsModDeclItem;

public class RsModDeclUsageInfo extends RsMoveUsageInfo {

    @Nonnull
    private final RsModDeclItem element;
    @Nonnull
    private final RsFile file;

    public RsModDeclUsageInfo(@Nonnull RsModDeclItem element, @Nonnull RsFile file) {
        super(element);
        this.element = element;
        this.file = file;
    }

    @Nonnull
    public RsModDeclItem getElement() {
        return element;
    }

    @Nonnull
    public RsFile getFile() {
        return file;
    }
}
