/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.RefreshQueue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

    public MacroExpansionVfsBatch(@Nonnull String contentRoot) {
        myContentRoot = contentRoot;
        myPathsToMarkDirty = new HashSet<>();
        myHasChanges = false;
        myExpansionFileSystem = MacroExpansionFileSystem.getInstance();
    }

    public boolean getHasChanges() {
        return myHasChanges;
    }

    @Nonnull
    public Path createFile(@Nonnull CratePersistentId crate, @Nonnull String expansionName, @Nonnull String content, boolean implicit) {
        String path = crate + "/" + MacroExpansionManagerUtil.expansionNameToPath(expansionName);
        return createFile(path, content, implicit);
    }

    @Nonnull
    public Path createFile(@Nonnull CratePersistentId crate, @Nonnull String expansionName, @Nonnull String content) {
        return createFile(crate, expansionName, content, false);
    }

    @Nonnull
    public Path createFile(@Nonnull String relativePath, @Nonnull String content, boolean implicit) {
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

    @Nonnull
    public Path createFile(@Nonnull String relativePath, @Nonnull String content) {
        return createFile(relativePath, content, false);
    }

    public void deleteFile(@Nonnull FSItem file) {
        file.delete();
        VirtualFile virtualFile = myExpansionFileSystem.findFileByPath(file.absolutePath());
        if (virtualFile != null) {
            markDirty(virtualFile);
        }
        myHasChanges = true;
    }

    
    public void deleteFile(@Nonnull VirtualFile file) {
        MacroExpansionFileSystem.getInstance().deleteFilePath(file.getPath());
        markDirty(file);
        myHasChanges = true;
    }

    
    public void writeFile(@Nonnull VirtualFile file, @Nonnull String content) {
        MacroExpansionFileSystem.getInstance().setFileContent(file.getPath(), content.getBytes());
        markDirty(file);
        myHasChanges = true;
    }

    public void applyToVfs(boolean async, @Nullable Runnable callback) {
        VirtualFile root = myExpansionFileSystem.findFileByPath("/");
        if (root == null) return;

        for (String path : myPathsToMarkDirty) {
            consulo.util.lang.Pair<VirtualFile, ?> result = org.rust.openapiext.OpenApiUtil.findNearestExistingFile(root, path);
            markDirty(result.getFirst());
        }

        RefreshQueue.getInstance().refresh(async, true, callback, root);
    }

    public void applyToVfs(boolean async) {
        applyToVfs(async, null);
    }

    private void markDirty(@Nonnull VirtualFile file) {
        VirtualFileUtil.markDirty(false, false, file);
    }

    public static class Path {
        private final String myPath;

        public Path(@Nonnull String path) {
            myPath = path;
        }

        @Nonnull
        public String getPath() {
            return myPath;
        }

        @Nullable
        public VirtualFile toVirtualFile() {
            return MacroExpansionFileSystem.getInstance().findFileByPath(myPath);
        }
    }
}
