/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.NotNull;
import org.rust.util.RsJacksonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FlatTreeJsonDeserializer extends RsJacksonDeserializer<FlatTree> {

    public static final FlatTreeJsonDeserializer INSTANCE = new FlatTreeJsonDeserializer();

    public FlatTreeJsonDeserializer() {
        super(FlatTree.class);
    }

    @NotNull
    @Override
    public FlatTree deserialize(@NotNull JsonParser parser, @NotNull DeserializationContext context) throws IOException {
        IntArrayList subtree = new IntArrayList();
        IntArrayList literal = new IntArrayList();
        IntArrayList punct = new IntArrayList();
        IntArrayList ident = new IntArrayList();
        IntArrayList tokenTree = new IntArrayList();
        List<String> text = new ArrayList<>();

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = parser.getCurrentName();
            parser.nextToken(); // move to value
            switch (fieldName) {
                case "subtree":
                    readIntArray(parser, subtree);
                    break;
                case "literal":
                    readIntArray(parser, literal);
                    break;
                case "punct":
                    readIntArray(parser, punct);
                    break;
                case "ident":
                    readIntArray(parser, ident);
                    break;
                case "token_tree":
                    readIntArray(parser, tokenTree);
                    break;
                case "text":
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        text.add(parser.getValueAsString());
                    }
                    break;
                default:
                    parser.skipChildren();
                    break;
            }
        }

        return new FlatTree(subtree, literal, punct, ident, tokenTree, text);
    }

    private static void readIntArray(@NotNull JsonParser parser, @NotNull IntArrayList out) throws IOException {
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            out.add((int) parser.getValueAsLong());
        }
    }
}
