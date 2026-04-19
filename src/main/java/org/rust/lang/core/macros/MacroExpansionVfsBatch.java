/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.lang.core.crate.CratePersistentId;
import org.rust.lang.core.macros.MacroExpansionFileSystem.FSItem;
import org.rust.openapiext.OpenApiUtil;

import java.util.HashSet;
import java.util.Set;

public class MacroExpansionVfsBatch {
    private final String myContentRoot;
    private final Set<String> myPathsToMarkDirty;
    private boolean myHasChanges;
    private final MacroExpansionFileSystem myExpansionFileSystem;

    public MacroExpansionVfsBatch(@NotNull String contentRoot) {
        myContentRoot = contentRoot;
        myPathsToMarkDirty = new HashSet<>();
        myHasChanges = false;
        myExpansionFileSystem = MacroExpansionFileSystem.getInstance();
    }

    public boolean getHasChanges() {
        return myHasChanges;
    }

    @NotNull
    public Path createFile(@NotNull CratePersistentId crate, @NotNull String expansionName, @NotNull String content, boolean implicit) {
        String path = crate + "/" + MacroExpansionManagerUtil.expansionNameToPath(expansionName);
        return createFile(path, content, implicit);
    }

    @NotNull
    public Path createFile(@NotNull CratePersistentId crate, @NotNull String expansionName, @NotNull String content) {
        return createFile(crate, expansionName, content, false);
    }

    @NotNull
    public Path createFile(@NotNull String relativePath, @NotNull String content, boolean implicit) {
        String path = myContentRoot + "/" + relativePath;
        if (implicit) {
            myExpansionFileSystem.createFileWithImplicitContent(path, content.getBytes().length, true);
        } else {
            myExpansionFileSystem.createFileWithExplicitContent(path, content.getBytes(), true);
        }
        String parent = path.substring(0, path.lastIndexOf('/'));
        myPathsToMarkDirty.add(parent);
        myHasChanges = true;
        return new Path(path);
    }

    @NotNull
    public Path createFile(@NotNull String relativePath, @NotNull String content) {
        return createFile(relativePath, content, false);
    }

    public void deleteFile(@NotNull FSItem file) {
        file.delete();
        VirtualFile virtualFile = myExpansionFileSystem.findFileByPath(file.absolutePath());
        if (virtualFile != null) {
            markDirty(virtualFile);
        }
        myHasChanges = true;
    }

    @TestOnly
    public void deleteFile(@NotNull VirtualFile file) {
        MacroExpansionFileSystem.getInstance().deleteFilePath(file.getPath());
        markDirty(file);
        myHasChanges = true;
    }

    @TestOnly
    public void writeFile(@NotNull VirtualFile file, @NotNull String content) {
        MacroExpansionFileSystem.getInstance().setFileContent(file.getPath(), content.getBytes());
        markDirty(file);
        myHasChanges = true;
    }

    public void applyToVfs(boolean async, @Nullable Runnable callback) {
        VirtualFile root = myExpansionFileSystem.findFileByPath("/");
        if (root == null) return;

        for (String path : myPathsToMarkDirty) {
            com.intellij.openapi.util.Pair<VirtualFile, ?> result = org.rust.openapiext.OpenApiUtil.findNearestExistingFile(root, path);
            markDirty(result.getFirst());
        }

        RefreshQueue.getInstance().refresh(async, true, callback, root);
    }

    public void applyToVfs(boolean async) {
        applyToVfs(async, null);
    }

    private void markDirty(@NotNull VirtualFile file) {
        VfsUtil.markDirty(false, false, file);
    }

    public static class Path {
        private final String myPath;

        public Path(@NotNull String path) {
            myPath = path;
        }

        @NotNull
        public String getPath() {
            return myPath;
        }

        @Nullable
        public VirtualFile toVirtualFile() {
            return MacroExpansionFileSystem.getInstance().findFileByPath(myPath);
        }
    }
}
