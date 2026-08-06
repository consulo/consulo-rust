/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.StubVirtualFile;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

/**
 * A name/parent pair standing in for a file that need not exist — used when asking a
 * {@code VirtualFileSystem} about a path.
 * <p>
 * Copy of {@code consulo.virtualFileSystem.internal.FakeVirtualFile}, which is exported only to
 * platform modules. Its superclass {@link StubVirtualFile} is public, so the subclass is a handful
 * of lines.
 */
public class RsFakeVirtualFile extends StubVirtualFile {

    private final VirtualFile myParent;
    private final String myName;

    public RsFakeVirtualFile(@Nonnull VirtualFile parent, @Nonnull String name) {
        myParent = parent;
        myName = name;
    }

    @Override
    public VirtualFile getParent() {
        return myParent;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Nonnull
    @Override
    public String getPath() {
        String basePath = myParent.getPath();
        return StringUtil.endsWithChar(basePath, '/') ? basePath + myName : basePath + '/' + myName;
    }

    @Nonnull
    @Override
    public String getName() {
        return myName;
    }

    @Override
    public String toString() {
        return getPath();
    }
}
