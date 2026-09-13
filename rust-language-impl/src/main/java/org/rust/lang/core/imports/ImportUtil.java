/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.imports;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.imports.ImportCandidate;
import org.rust.lang.core.imports.ImportUtil;

/**
 * Bridge class providing Java-accessible import operations.
 *
 * (Java reserved keyword). This bridge in the 'imports' (plural) package delegates
 */
public final class ImportUtil {
    private ImportUtil() {
    }

    /**
     * Imports the given candidate at the context element location.
     *
     * @param candidate the import candidate to import
     * @param context   the PSI element where the import should be added
     */
    public static void import_(@Nonnull ImportCandidate candidate, @Nonnull RsElement context) {
        ImportBridge.importCandidate(candidate, context);
    }

    /**
     * Imports the given candidate at the context element location.
     * Same as {@link #import_}, but named {@code doImport} for clarity.
     *
     * @param candidate the import candidate to import
     * @param context   the PSI element where the import should be added
     */
    public static void doImport(@Nonnull ImportCandidate candidate, @Nonnull RsElement context) {
        ImportBridge.importCandidate(candidate, context);
    }
}
