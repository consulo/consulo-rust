/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import consulo.virtualFileSystem.VirtualFile;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Bridge class delegating to {@link Utils}, {@link CollectionsKt}, {@link IoKt}, {@link PathKt}.
 */
public final class StdextUtil {
    private StdextUtil() {
    }

    public static <T> T applyWithSymlink(@Nonnull VirtualFile file, @Nonnull Function<VirtualFile, T> f) {
        return Utils.applyWithSymlink(file, f);
    }

    public static boolean isPowerOfTwo(long value) {
        return Utils.isPowerOfTwo(value);
    }

    @Nonnull
    public static String pluralize(@Nonnull String str) {
        return Utils.pluralize(str);
    }

    public static <T> List<T> optimizeList(@Nonnull SmartList<T> list) {
        return CollectionsUtil.optimizeList(list);
    }

    public static <T> T nextOrNull(@Nonnull Iterator<T> iterator) {
        return CollectionsUtil.nextOrNull(iterator);
    }

    public static int readVarInt(@Nonnull DataInput input) throws IOException {
        return IoUtil.readVarInt(input);
    }

    public static void writeVarInt(@Nonnull DataOutput output, int value) throws IOException {
        IoUtil.writeVarInt(output, value);
    }

    @Nonnull
    public static Path toPath(@Nonnull String str) {
        return PathUtil.toPath(str);
    }
}
