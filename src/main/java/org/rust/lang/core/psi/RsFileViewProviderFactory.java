/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.lang.Language;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.FileViewProviderFactory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Hacky adjust the file limit for Rust file.
 * Coupled with {@code org.rust.lang.core.resolve.indexes.RsAliasIndex.getFileTypesWithSizeLimitNotApplicable}.
 *
 * @see SingleRootFileViewProvider#isTooLargeForIntelligence
 */
public class RsFileViewProviderFactory implements FileViewProviderFactory {

    // Experimentally verified that 8Mb works with the default IDEA -Xmx768M
    private static final int RUST_FILE_SIZE_LIMIT_FOR_INTELLISENSE = 8 * 1024 * 1024;

    @NotNull
    @Override
    public FileViewProvider createFileViewProvider(@NotNull VirtualFile file,
                                                    Language language,
                                                    @NotNull PsiManager manager,
                                                    boolean eventSystemEnabled) {
        boolean shouldAdjustFileLimit = SingleRootFileViewProvider.isTooLargeForIntelligence(file)
            && file.getLength() <= RUST_FILE_SIZE_LIMIT_FOR_INTELLISENSE;

        if (shouldAdjustFileLimit) {
            SingleRootFileViewProvider.doNotCheckFileSizeLimit(file);
        }

        return new SingleRootFileViewProvider(manager, file, eventSystemEnabled);
    }
}
