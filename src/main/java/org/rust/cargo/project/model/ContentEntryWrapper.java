/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class ContentEntryWrapper {

    private final ContentEntry contentEntry;
    private final Set<String> knownFolders;

    public ContentEntryWrapper(@NotNull ContentEntry contentEntry) {
        this.contentEntry = contentEntry;
        this.knownFolders = computeKnownFolders(contentEntry);
    }

    public void addExcludeFolder(@NotNull String url) {
        if (knownFolders.contains(url)) return;
        contentEntry.addExcludeFolder(url);
    }

    public void addSourceFolder(@NotNull String url, boolean isTestSource) {
        if (knownFolders.contains(url)) return;
        contentEntry.addSourceFolder(url, isTestSource);
    }

    public void setup(@NotNull VirtualFile contentRoot) {
        CargoProjectServiceUtil.setup(this, contentRoot);
    }

    private static Set<String> computeKnownFolders(@NotNull ContentEntry contentEntry) {
        Set<String> knownRoots = new HashSet<>();
        for (var sf : contentEntry.getSourceFolders()) {
            knownRoots.add(sf.getUrl());
        }
        for (String url : contentEntry.getExcludeFolderUrls()) {
            knownRoots.add(url);
        }
        return knownRoots;
    }
}
