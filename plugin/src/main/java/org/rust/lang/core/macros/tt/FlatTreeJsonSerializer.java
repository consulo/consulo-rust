/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import jakarta.annotation.Nonnull;
import org.rust.util.RsJacksonSerializer;

import java.io.IOException;
import java.util.List;

public class FlatTreeJsonSerializer extends RsJacksonSerializer<FlatTree> {

    public static final FlatTreeJsonSerializer INSTANCE = new FlatTreeJsonSerializer();

    public FlatTreeJsonSerializer() {
        super(FlatTree.class);
    }

    @Override
    public void serialize(@Nonnull FlatTree tt, @Nonnull JsonGenerator gen, @Nonnull SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        writeIntArray(gen, "subtree", tt.getSubtree());
        writeIntArray(gen, "literal", tt.getLiteral());
        writeIntArray(gen, "punct", tt.getPunct());
        writeIntArray(gen, "ident", tt.getIdent());
        writeIntArray(gen, "token_tree", tt.getTokenTree());
        writeStringArray(gen, "text", tt.getText());
        gen.writeEndObject();
    }

    private static void writeIntArray(@Nonnull JsonGenerator gen, @Nonnull String fieldName, @Nonnull IntArrayList arr) throws IOException {
        gen.writeFieldName(fieldName);
        gen.writeStartArray();
        for (int i = 0; i < arr.size(); i++) {
            gen.writeNumber(arr.getInt(i) & 0xFFFFFFFFL);
        }
        gen.writeEndArray();
    }

    private static void writeStringArray(@Nonnull JsonGenerator gen, @Nonnull String fieldName, @Nonnull List<String> list) throws IOException {
        gen.writeFieldName(fieldName);
        gen.writeStartArray();
        for (String s : list) {
            gen.writeString(s);
        }
        gen.writeEndArray();
    }
}
