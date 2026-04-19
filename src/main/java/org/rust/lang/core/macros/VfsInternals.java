/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl;
import com.intellij.util.BitUtil;
import com.intellij.util.io.DigestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.HashCode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class VfsInternals {
    public static final VfsInternals INSTANCE = new VfsInternals();

    private VfsInternals() {
    }

    /** {@code com.intellij.openapi.vfs.newvfs.persistent.PersistentFS.Flags.MUST_RELOAD_CONTENT} */
    @VisibleForTesting
    public static final int MUST_RELOAD_CONTENT = 0x08;

    /** {@code com.intellij.openapi.vfs.newvfs.persistent.PersistentFSContentAccessor.getContentHashDigest} */
    @NotNull
    private static MessageDigest getContentHashDigest() {
        return DigestUtil.sha1();
    }

    @VisibleForTesting
    public static boolean isMarkedForContentReload(@NotNull VirtualFile file) {
        return BitUtil.isSet(PersistentFS.getInstance().getFileAttributes(org.rust.openapiext.OpenApiUtil.getFileId(file)), MUST_RELOAD_CONTENT);
    }

    @VisibleForTesting
    public static void reloadFileIfNeeded(@NotNull VirtualFile file) throws IOException {
        if (isMarkedForContentReload(file)) {
            file.contentsToByteArray(false);
        }
    }

    /** {@code null} means disabled hashing or invalid file */
    @VisibleForTesting
    @Nullable
    public static HashCode getContentHashIfStored(@NotNull VirtualFile file) {
        byte[] hash = PersistentFSImpl.getContentHashIfStored(file);
        if (hash != null) {
            return HashCode.fromByteArray(hash);
        }
        return null;
    }

    /** {@code com.intellij.openapi.vfs.newvfs.persistent.PersistentFSContentAccessor.calculateHash} */
    @NotNull
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

            public Ok(@NotNull HashCode hash) {
                myHash = hash;
            }

            @NotNull
            public HashCode getHash() {
                return myHash;
            }
        }

        public static final class Err extends ContentHashResult {
            private final IOException myError;

            public Err(@NotNull IOException error) {
                myError = error;
            }

            @NotNull
            public IOException getError() {
                return myError;
            }
        }
    }
}
