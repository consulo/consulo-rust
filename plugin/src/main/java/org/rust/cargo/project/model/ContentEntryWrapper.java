/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.module.content.layer.ContentEntry;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;

/**
 * Wraps {@link ContentEntry} to provide IntelliJ-style addSourceFolder/addExcludeFolder helpers.
 * Consulo's ContentEntry API is different (ContentFolderTypeProvider-based); these methods no-op
 * for now — the Rust module builder will need to wire them properly via ContentFolderTypeProvider.
 */
public class ContentEntryWrapper {

    private final ContentEntry contentEntry;
    private final Set<String> knownFolders;

    public ContentEntryWrapper(@Nonnull ContentEntry contentEntry) {
        this.contentEntry = contentEntry;
        this.knownFolders = new HashSet<>();
    }

    public void addExcludeFolder(@Nonnull String url) {
        knownFolders.add(url);
        // TODO: map to ContentFolderTypeProvider-based addFolder
    }

    public void addSourceFolder(@Nonnull String url, boolean isTestSource) {
        knownFolders.add(url);
        // TODO: map to ContentFolderTypeProvider-based addFolder
    }

    public void setup(@Nonnull VirtualFile contentRoot) {
        CargoProjectServiceUtil.setup(this, contentRoot);
    }
}
