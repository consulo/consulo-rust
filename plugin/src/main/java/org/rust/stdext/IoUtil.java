/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import consulo.util.io.FileUtil;
import consulo.index.io.data.DataInputOutputUtil;
import jakarta.annotation.Nonnull;

import java.io.*;

public final class IoUtil {
    private IoUtil() {
    }

    public static int readVarInt(@Nonnull DataInput input) throws IOException {
        return DataInputOutputUtil.readINT(input);
    }

    public static void writeVarInt(@Nonnull DataOutput output, int value) throws IOException {
        DataInputOutputUtil.writeINT(output, value);
    }

    public static void writeStream(@Nonnull OutputStream output, @Nonnull InputStream input) throws IOException {
        FileUtil.copy(input, output);
    }

    public static <E extends Enum<E>> void writeEnum(@Nonnull DataOutput output, @Nonnull E e) throws IOException {
        output.writeByte(e.ordinal());
    }

    @Nonnull
    public static <E extends Enum<E>> E readEnum(@Nonnull DataInput input, @Nonnull Class<E> enumClass) throws IOException {
        return enumClass.getEnumConstants()[input.readUnsignedByte()];
    }

    @Nonnull
    public static <T, E> RsResult<T, E> readRsResult(
        @Nonnull DataInput input,
        @Nonnull DataInputReader<T> okReader,
        @Nonnull DataInputReader<E> errReader
    ) throws IOException {
        if (input.readBoolean()) {
            return new RsResult.Ok<>(okReader.read(input));
        } else {
            return new RsResult.Err<>(errReader.read(input));
        }
    }

    public static <T, E> void writeRsResult(
        @Nonnull DataOutput output,
        @Nonnull RsResult<T, E> value,
        @Nonnull DataOutputWriter<T> okWriter,
        @Nonnull DataOutputWriter<E> errWriter
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
        T read(@Nonnull DataInput input) throws IOException;
    }

    @FunctionalInterface
    public interface DataOutputWriter<T> {
        void write(@Nonnull DataOutput output, T value) throws IOException;
    }
}
