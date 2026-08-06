/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.language.Language;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.file.FileViewProvider;
import consulo.language.file.FileViewProviderFactory;
import consulo.language.psi.PsiManager;
import consulo.language.impl.file.SingleRootFileViewProvider;
import jakarta.annotation.Nonnull;

/**
 * Hacky adjust the file limit for Rust file.
 * Coupled with {@code org.rust.lang.core.resolve.indexes.RsAliasIndex.getFileTypesWithSizeLimitNotApplicable}.
 *
 * @see SingleRootFileViewProvider#isTooLargeForIntelligence
 */
public class RsFileViewProviderFactory implements FileViewProviderFactory {

    // Experimentally verified that 8Mb works with the default IDEA -Xmx768M
    private static final int RUST_FILE_SIZE_LIMIT_FOR_INTELLISENSE = 8 * 1024 * 1024;

    @Nonnull
    @Override
    public FileViewProvider createFileViewProvider(@Nonnull VirtualFile file,
                                                    Language language,
                                                    @Nonnull PsiManager manager,
                                                    boolean eventSystemEnabled) {
        boolean shouldAdjustFileLimit = SingleRootFileViewProvider.isTooLargeForIntelligence(file)
            && file.getLength() <= RUST_FILE_SIZE_LIMIT_FOR_INTELLISENSE;

        if (shouldAdjustFileLimit) {
            SingleRootFileViewProvider.doNotCheckFileSizeLimit(file);
        }

        return new SingleRootFileViewProvider(manager, file, eventSystemEnabled);
    }
}
