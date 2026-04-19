/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils.imports;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Bridge class delegating to {@link ImportInfo} operations.
 *
 * lives in the 'org.rust.ide.utils.import' package (Java reserved keyword). This bridge in
 */
public final class ImportInfoUtil {
    private ImportInfoUtil() {
    }

    /**
     * Inserts an {@code extern crate} item if the crate of importing element
     * differs from the crate of {@code context}.
     *
     * @param importInfo the import info (must be an {@link ImportInfo} instance)
     * @param context    the PSI element providing the crate context
     */
    public static void insertExternCrateIfNeeded(@NotNull ImportInfo importInfo, @NotNull RsElement context) {
        importInfo.insertExternCrateIfNeeded(context);
    }
}
