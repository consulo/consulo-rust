/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.io.DataInputOutputUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;

public final class IoUtil {
    private IoUtil() {
    }

    public static int readVarInt(@NotNull DataInput input) throws IOException {
        return DataInputOutputUtil.readINT(input);
    }

    public static void writeVarInt(@NotNull DataOutput output, int value) throws IOException {
        DataInputOutputUtil.writeINT(output, value);
    }

    public static void writeStream(@NotNull OutputStream output, @NotNull InputStream input) throws IOException {
        FileUtil.copy(input, output);
    }

    public static <E extends Enum<E>> void writeEnum(@NotNull DataOutput output, @NotNull E e) throws IOException {
        output.writeByte(e.ordinal());
    }

    @NotNull
    public static <E extends Enum<E>> E readEnum(@NotNull DataInput input, @NotNull Class<E> enumClass) throws IOException {
        return enumClass.getEnumConstants()[input.readUnsignedByte()];
    }

    @NotNull
    public static <T, E> RsResult<T, E> readRsResult(
        @NotNull DataInput input,
        @NotNull DataInputReader<T> okReader,
        @NotNull DataInputReader<E> errReader
    ) throws IOException {
        if (input.readBoolean()) {
            return new RsResult.Ok<>(okReader.read(input));
        } else {
            return new RsResult.Err<>(errReader.read(input));
        }
    }

    public static <T, E> void writeRsResult(
        @NotNull DataOutput output,
        @NotNull RsResult<T, E> value,
        @NotNull DataOutputWriter<T> okWriter,
        @NotNull DataOutputWriter<E> errWriter
    ) throws IOException {
        if (value instanceof RsResult.Ok) {
            output.writeBoolean(true);
            okWriter.write(output, ((RsResult.Ok<T, E>) value).getOk());
        } else if (value instanceof RsResult.Err) {
            output.writeBoolean(false);
            errWriter.write(output, ((RsResult.Err<T, E>) value).getErr());
        }
    }

    @FunctionalInterface
    public interface DataInputReader<T> {
        T read(@NotNull DataInput input) throws IOException;
    }

    @FunctionalInterface
    public interface DataOutputWriter<T> {
        void write(@NotNull DataOutput output, T value) throws IOException;
    }
}
