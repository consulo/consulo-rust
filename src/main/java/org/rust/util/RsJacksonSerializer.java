/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.IntConsumer;

public abstract class RsJacksonSerializer<T> extends StdSerializer<T> {
    protected RsJacksonSerializer(@NotNull Class<T> t) {
        super(t);
    }

    protected void writeJsonObject(@NotNull JsonGenerator gen, @NotNull JsonGeneratorAction action) throws IOException {
        gen.writeStartObject();
        action.execute(gen);
        gen.writeEndObject();
    }

    protected void writeJsonObjectWithSingleField(@NotNull JsonGenerator gen, @NotNull String name, @NotNull JsonGeneratorAction action) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName(name);
        action.execute(gen);
        gen.writeEndObject();
    }

    protected <E> void writeArrayField(@NotNull JsonGenerator gen, @NotNull String name, @NotNull Iterable<E> list, @NotNull ElementWriter<E> writer) throws IOException {
        gen.writeFieldName(name);
        writeArray(gen, list, writer);
    }

    protected <E> void writeArray(@NotNull JsonGenerator gen, @NotNull Iterable<E> list, @NotNull ElementWriter<E> writer) throws IOException {
        gen.writeStartArray();
        for (E e : list) {
            writer.write(gen, e);
        }
        gen.writeEndArray();
    }

    protected void writeArrayField(@NotNull JsonGenerator gen, @NotNull String name, @NotNull IntArrayList list, @NotNull IntElementWriter writer) throws IOException {
        gen.writeFieldName(name);
        gen.writeStartArray();
        list.forEach((IntConsumer) value -> {
            try {
                writer.write(gen, value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        gen.writeEndArray();
    }

    protected <V> void writeNullableField(@NotNull JsonGenerator gen, @NotNull String name, @Nullable V value, @NotNull ElementWriter<V> writer) throws IOException {
        gen.writeFieldName(name);
        if (value != null) {
            writer.write(gen, value);
        } else {
            gen.writeNull();
        }
    }

    @FunctionalInterface
    protected interface JsonGeneratorAction {
        void execute(@NotNull JsonGenerator gen) throws IOException;
    }

    @FunctionalInterface
    protected interface ElementWriter<E> {
        void write(@NotNull JsonGenerator gen, E element) throws IOException;
    }

    @FunctionalInterface
    protected interface IntElementWriter {
        void write(@NotNull JsonGenerator gen, int element) throws IOException;
    }
}
