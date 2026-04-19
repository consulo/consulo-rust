/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

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

    public static <T> T applyWithSymlink(@NotNull VirtualFile file, @NotNull Function<VirtualFile, T> f) {
        return Utils.applyWithSymlink(file, f);
    }

    public static boolean isPowerOfTwo(long value) {
        return Utils.isPowerOfTwo(value);
    }

    @NotNull
    public static String pluralize(@NotNull String str) {
        return Utils.pluralize(str);
    }

    public static <T> List<T> optimizeList(@NotNull SmartList<T> list) {
        return CollectionsUtil.optimizeList(list);
    }

    public static <T> T nextOrNull(@NotNull Iterator<T> iterator) {
        return CollectionsUtil.nextOrNull(iterator);
    }

    public static int readVarInt(@NotNull DataInput input) throws IOException {
        return IoUtil.readVarInt(input);
    }

    public static void writeVarInt(@NotNull DataOutput output, int value) throws IOException {
        IoUtil.writeVarInt(output, value);
    }

    @NotNull
    public static Path toPath(@NotNull String str) {
        return PathUtil.toPath(str);
    }
}
