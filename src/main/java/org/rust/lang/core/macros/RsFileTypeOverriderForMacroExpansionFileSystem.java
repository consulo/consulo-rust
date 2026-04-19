/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.impl.FileTypeOverrider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.StubVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsFileType;

/**
 * Sets {@link FileType} to {@link RsFileType} for each file in {@link MacroExpansionFileSystem}.
 * Used only as an optimization: default file type detection is slow, but we
 * know that any file in {@link MacroExpansionFileSystem} is {@link RsFileType}
 */
@SuppressWarnings("UnstableApiUsage")
public class RsFileTypeOverriderForMacroExpansionFileSystem implements FileTypeOverrider {
    @Nullable
    @Override
    public FileType getOverriddenFileType(@NotNull VirtualFile file) {
        if (!(file instanceof StubVirtualFile) && !file.isDirectory() && file.getFileSystem() instanceof MacroExpansionFileSystem) {
            return RsFileType.INSTANCE;
        } else {
            return null;
        }
    }
}
