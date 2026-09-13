/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.imports;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.imports.ImportCandidate;
import org.rust.lang.core.imports.ImportCandidateUtil;

/**
 * Bridge class delegating to {@link ImportCandidate} import operations.
 *
 * 'org.rust.ide.utils.import' package (Java reserved keyword). This bridge in the
 */
public final class ImportCandidateUtil {
    private ImportCandidateUtil() {
    }

    /**
     * Imports the given candidate at the context element location.
     *
     * @param candidate the import candidate to import
     * @param context   the PSI element where the import should be added
     */
    public static void doImport(@Nonnull ImportCandidate candidate, @Nonnull RsElement context) {
        ImportBridge.importCandidate(candidate, context);
    }
}
