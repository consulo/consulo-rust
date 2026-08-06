/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.ManagingFS;
import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.NewVirtualFileSystem;
import consulo.virtualFileSystem.RefreshQueue;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.BiFunction;

/**
 * Path resolution helpers for a custom {@link NewVirtualFileSystem}: locate the root via
 * {@link NewVirtualFileSystem#extractRootPath}, then walk the remaining path segments.
 * <p>
 * Reimplementation of {@code consulo.virtualFileSystem.internal.VfsImplUtil}, which is exported only
 * to platform modules. Every building block it uses — {@link ManagingFS}, {@link RefreshQueue},
 * {@link NewVirtualFile}, {@link VFileProperty} — is public, so only the walk itself is re-created.
 * Only the four entry points {@link MacroExpansionFileSystem} needs are provided.
 */
public final class RsVfsImplUtil {

    private static final String FILE_SEPARATORS = "/" + java.io.File.separatorChar;

    private RsVfsImplUtil() {
    }

    @Nullable
    public static NewVirtualFile findFileByPath(@Nonnull NewVirtualFileSystem vfs, @Nonnull String path) {
        return walk(vfs, path, NewVirtualFile::findChild);
    }

    @Nullable
    public static NewVirtualFile findFileByPathIfCached(@Nonnull NewVirtualFileSystem vfs, @Nonnull String path) {
        return walk(vfs, path, NewVirtualFile::findChildIfCached);
    }

    @Nullable
    public static NewVirtualFile refreshAndFindFileByPath(@Nonnull NewVirtualFileSystem vfs, @Nonnull String path) {
        return walk(vfs, path, NewVirtualFile::refreshAndFindChild);
    }

    public static void refresh(@Nonnull NewVirtualFileSystem vfs, boolean asynchronous) {
        VirtualFile[] roots = ManagingFS.getInstance().getRoots(vfs);
        if (roots.length > 0) {
            RefreshQueue.getInstance().refresh(asynchronous, true, null, roots);
        }
    }

    /**
     * @param findChild how to descend one segment — differs per entry point (cached lookup,
     *                  plain lookup, or refreshing lookup)
     */
    @Nullable
    private static NewVirtualFile walk(@Nonnull NewVirtualFileSystem vfs,
                                       @Nonnull String path,
                                       @Nonnull BiFunction<NewVirtualFile, String, NewVirtualFile> findChild) {
        String normalizedPath = vfs.normalize(path);
        if (StringUtil.isEmptyOrSpaces(normalizedPath)) return null;

        String basePath = vfs.extractRootPath(normalizedPath);
        if (basePath.length() > normalizedPath.length()) return null;

        NewVirtualFile file = ManagingFS.getInstance().findRoot(basePath, vfs);
        if (file == null || !file.exists()) return null;

        for (String pathElement : StringUtil.tokenize(normalizedPath.substring(basePath.length()), FILE_SEPARATORS)) {
            if (pathElement.isEmpty() || ".".equals(pathElement)) continue;

            if ("..".equals(pathElement)) {
                if (file.is(VFileProperty.SYMLINK)) {
                    NewVirtualFile canonicalFile = file.getCanonicalFile();
                    file = canonicalFile != null ? canonicalFile.getParent() : null;
                }
                else {
                    file = file.getParent();
                }
            }
            else {
                file = findChild.apply(file, pathElement);
            }

            if (file == null) return null;
        }

        return file;
    }
}
