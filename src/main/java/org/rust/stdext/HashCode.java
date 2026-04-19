/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import com.intellij.util.io.DigestUtil;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;

public final class HashCode implements Serializable {
    public static final int ARRAY_LEN = 20;

    private static final ThreadLocal<MessageDigest> SHA1 = ThreadLocal.withInitial(DigestUtil::sha1);

    private final byte[] myHash;

    private HashCode(@NotNull byte[] hash) {
        this.myHash = hash;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        HashCode hashCode = (HashCode) other;
        return Arrays.equals(myHash, hashCode.myHash);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(myHash);
    }

    @Override
    @NotNull
    public String toString() {
        return Hex.encodeHexString(myHash);
    }

    @NotNull
    public byte[] toByteArray() {
        return myHash;
    }

    @NotNull
    public static HashCode compute(@NotNull String input) {
        return new HashCode(SHA1.get().digest(input.getBytes()));
    }

    @NotNull
    public static HashCode compute(@NotNull DataOutputConsumer computation) throws IOException {
        MessageDigest sha1 = SHA1.get();
        DataOutputStream dos = new DataOutputStream(new DigestOutputStream(OutputStream.nullOutputStream(), sha1));
        computation.accept(dos);
        return new HashCode(sha1.digest());
    }

    @NotNull
    public static HashCode ofFile(@NotNull Path path) throws IOException {
        MessageDigest digest = SHA1.get();
        try (DigestInputStream dis = new DigestInputStream(Files.newInputStream(path), digest)) {
            IoUtil.writeStream(OutputStream.nullOutputStream(), dis);
        }
        return new HashCode(digest.digest());
    }

    @NotNull
    public static HashCode mix(@NotNull HashCode hash1, @NotNull HashCode hash2) {
        MessageDigest md = SHA1.get();
        md.update(hash1.toByteArray());
        md.update(hash2.toByteArray());
        return new HashCode(md.digest());
    }

    @NotNull
    public static HashCode fromByteArray(@NotNull byte[] bytes) {
        if (bytes.length != ARRAY_LEN) {
            throw new IllegalStateException("Expected " + ARRAY_LEN + " bytes, got " + bytes.length);
        }
        return new HashCode(bytes);
    }

    @NotNull
    public static HashCode fromHexString(@NotNull String hex) throws Exception {
        byte[] bytes = Hex.decodeHex(hex);
        if (bytes.length != ARRAY_LEN) {
            throw new IllegalStateException("Expected " + ARRAY_LEN + " bytes, got " + bytes.length);
        }
        return new HashCode(bytes);
    }

    public static void writeHashCode(@NotNull DataOutput output, @NotNull HashCode hash) throws IOException {
        output.write(hash.toByteArray());
    }

    @NotNull
    public static HashCode readHashCode(@NotNull DataInput input) throws IOException {
        byte[] bytes = new byte[ARRAY_LEN];
        input.readFully(bytes);
        return fromByteArray(bytes);
    }

    public static void writeHashCodeNullable(@NotNull DataOutput output, @Nullable HashCode hash) throws IOException {
        if (hash == null) {
            output.writeBoolean(false);
        } else {
            output.writeBoolean(true);
            writeHashCode(output, hash);
        }
    }

    @Nullable
    public static HashCode readHashCodeNullable(@NotNull DataInput input) throws IOException {
        if (input.readBoolean()) {
            return readHashCode(input);
        }
        return null;
    }

    @FunctionalInterface
    public interface DataOutputConsumer {
        void accept(@NotNull DataOutputStream out) throws IOException;
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.SOURCE)
    private @interface Nullable {
    }
}
