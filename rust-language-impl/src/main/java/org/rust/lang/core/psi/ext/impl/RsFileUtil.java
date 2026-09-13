/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiFile;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.ext.*;

/**
 * Utility methods for RsFile.
 */
public final class RsFileUtil {
    private RsFileUtil() {
    }

    @Nullable
    public static RsFile getRustFile(@Nullable PsiFile file) {
        return file instanceof RsFile ? (RsFile) file : null;
    }
}
