/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.virtualFileSystem.VirtualFile;
import consulo.util.io.DigestUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.stdext.HashCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class VfsInternals {
    public static final VfsInternals INSTANCE = new VfsInternals();

    private VfsInternals() {
    }

    /** {@code com.intellij.openapi.vfs.newvfs.persistent.PersistentFSContentAccessor.getContentHashDigest} */
    @Nonnull
    private static MessageDigest getContentHashDigest() {
        return DigestUtil.sha1();
    }

    /**
     * Upstream only re-read the file when the VFS had flagged it {@code MUST_RELOAD_CONTENT}. Consulo
     * keeps that flag in {@code consulo.virtualFileSystem.internal.PersistentFS}, which is exported
     * only to platform modules, so a plugin cannot query it. Reading unconditionally is a superset of
     * the upstream behaviour — correct, just occasionally redundant I/O. This is a
     * {@code @VisibleForTesting} helper upstream with no production callers, so the cost is moot.
     */
    public static void reloadFileIfNeeded(@Nonnull VirtualFile file) throws IOException {
        file.contentsToByteArray(false);
    }

    /** {@code null} means disabled hashing or invalid file */
    
    @Nullable
    public static HashCode getContentHashIfStored(@Nonnull VirtualFile file) {
        // PersistentFSImpl.getContentHashIfStored is not exposed in Consulo; return null (no cached hash)
        return null;
    }

    /** {@code com.intellij.openapi.vfs.newvfs.persistent.PersistentFSContentAccessor.calculateHash} */
    @Nonnull
    public static HashCode calculateContentHash(byte[] fileContent) {
        MessageDigest digest = getContentHashDigest();
        digest.update(String.valueOf(fileContent.length).getBytes(StandardCharsets.UTF_8));
        digest.update("\0".getBytes(StandardCharsets.UTF_8));
        digest.update(fileContent);
        return HashCode.fromByteArray(digest.digest());
    }

    public abstract static class ContentHashResult {
        public static final class Disabled extends ContentHashResult {
            public static final Disabled INSTANCE = new Disabled();
            private Disabled() {}
        }

        public static final class Ok extends ContentHashResult {
            private final HashCode myHash;

            public Ok(@Nonnull HashCode hash) {
                myHash = hash;
            }

            @Nonnull
            public HashCode getHash() {
                return myHash;
            }
        }

        public static final class Err extends ContentHashResult {
            private final IOException myError;

            public Err(@Nonnull IOException error) {
                myError = error;
            }

            @Nonnull
            public IOException getError() {
                return myError;
            }
        }
    }
}
