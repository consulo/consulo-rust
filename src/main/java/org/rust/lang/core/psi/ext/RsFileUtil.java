/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFile;

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
