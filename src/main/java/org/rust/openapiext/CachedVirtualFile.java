/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class CachedVirtualFile {
    @Nullable
    private final String url;
    private final AtomicReference<VirtualFile> cache = new AtomicReference<>();

    public CachedVirtualFile(@Nullable String url) {
        this.url = url;
    }

    @Nullable
    public VirtualFile getValue() {
        if (url == null) return null;
        VirtualFile cached = cache.get();
        if (cached != null && cached.isValid()) return cached;
        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(url);
        cache.set(file);
        return file;
    }
}
