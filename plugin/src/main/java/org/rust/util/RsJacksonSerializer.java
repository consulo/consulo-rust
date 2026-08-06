/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.function.IntConsumer;

public abstract class RsJacksonSerializer<T> extends StdSerializer<T> {
    protected RsJacksonSerializer(@Nonnull Class<T> t) {
        super(t);
    }

    protected void writeJsonObject(@Nonnull JsonGenerator gen, @Nonnull JsonGeneratorAction action) throws IOException {
        gen.writeStartObject();
        action.execute(gen);
        gen.writeEndObject();
    }

    protected void writeJsonObjectWithSingleField(@Nonnull JsonGenerator gen, @Nonnull String name, @Nonnull JsonGeneratorAction action) throws IOException {
        gen.writeStartObject();
        gen.writeFieldName(name);
        action.execute(gen);
        gen.writeEndObject();
    }

    protected <E> void writeArrayField(@Nonnull JsonGenerator gen, @Nonnull String name, @Nonnull Iterable<E> list, @Nonnull ElementWriter<E> writer) throws IOException {
        gen.writeFieldName(name);
        writeArray(gen, list, writer);
    }

    protected <E> void writeArray(@Nonnull JsonGenerator gen, @Nonnull Iterable<E> list, @Nonnull ElementWriter<E> writer) throws IOException {
        gen.writeStartArray();
        for (E e : list) {
            writer.write(gen, e);
        }
        gen.writeEndArray();
    }

    protected void writeArrayField(@Nonnull JsonGenerator gen, @Nonnull String name, @Nonnull IntArrayList list, @Nonnull IntElementWriter writer) throws IOException {
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

    protected <V> void writeNullableField(@Nonnull JsonGenerator gen, @Nonnull String name, @Nullable V value, @Nonnull ElementWriter<V> writer) throws IOException {
        gen.writeFieldName(name);
        if (value != null) {
            writer.write(gen, value);
        } else {
            gen.writeNull();
        }
    }

    @FunctionalInterface
    protected interface JsonGeneratorAction {
        void execute(@Nonnull JsonGenerator gen) throws IOException;
    }

    @FunctionalInterface
    protected interface ElementWriter<E> {
        void write(@Nonnull JsonGenerator gen, E element) throws IOException;
    }

    @FunctionalInterface
    protected interface IntElementWriter {
        void write(@Nonnull JsonGenerator gen, int element) throws IOException;
    }
}
