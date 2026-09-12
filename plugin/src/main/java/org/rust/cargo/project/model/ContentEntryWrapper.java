/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.content.ContentFolderTypeProvider;
import consulo.content.base.ExcludedContentFolderTypeProvider;
import consulo.language.content.ProductionContentFolderTypeProvider;
import consulo.language.content.TestContentFolderTypeProvider;
import consulo.module.content.layer.ContentEntry;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Declares Cargo layout directories on a {@link ContentEntry}. Folders the entry already
 * declares are left untouched, so running the setup over a configured module is a no-op.
 */
public class ContentEntryWrapper {

    private final ContentEntry myContentEntry;
    private final Set<String> myKnownFolders;

    public ContentEntryWrapper(@Nonnull ContentEntry contentEntry) {
        myContentEntry = contentEntry;
        myKnownFolders = new LinkedHashSet<>();
        Collections.addAll(myKnownFolders, contentEntry.getFolderUrls(provider -> true));
    }

    public void addExcludeFolder(@Nonnull String url) {
        addFolder(url, ExcludedContentFolderTypeProvider.getInstance());
    }

    public void addSourceFolder(@Nonnull String url, boolean isTestSource) {
        addFolder(url, isTestSource
            ? TestContentFolderTypeProvider.getInstance()
            : ProductionContentFolderTypeProvider.getInstance());
    }

    private void addFolder(@Nonnull String url, @Nonnull ContentFolderTypeProvider typeProvider) {
        if (!myKnownFolders.add(url)) {
            return;
        }
        myContentEntry.addFolder(url, typeProvider);
    }

    public void setup(@Nonnull VirtualFile contentRoot) {
        CargoProjectServiceUtil.setup(this, contentRoot);
    }
}
