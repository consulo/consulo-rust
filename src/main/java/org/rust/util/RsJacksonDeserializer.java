/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class RsJacksonDeserializer<T> extends StdDeserializer<T> {
    protected RsJacksonDeserializer(@NotNull Class<T> vc) {
        super(vc);
    }

    protected void expectToken(@NotNull DeserializationContext ctx, @NotNull JsonToken expectedToken) throws IOException {
        if (ctx.getParser().currentToken() != expectedToken) {
            ctx.reportWrongTokenException(handledType(), expectedToken, null);
        }
    }

    protected void expectNextToken(@NotNull DeserializationContext ctx, @NotNull JsonToken expectedToken) throws IOException {
        if (ctx.getParser().nextToken() != expectedToken) {
            ctx.reportWrongTokenException(handledType(), expectedToken, null);
        }
    }

    @NotNull
    protected String expectNextFieldName(@NotNull DeserializationContext ctx) throws IOException {
        String name = ctx.getParser().nextFieldName();
        if (name == null) {
            ctx.reportWrongTokenException(handledType(), JsonToken.FIELD_NAME, null);
            throw new IllegalStateException("unreachable");
        }
        return name;
    }

    @NotNull
    protected String readString(@NotNull DeserializationContext ctx) throws IOException {
        return _parseString(ctx.getParser(), ctx);
    }

    protected long readLong(@NotNull DeserializationContext ctx) throws IOException {
        return _parseLongPrimitive(ctx.getParser(), ctx);
    }

    protected <V> V readValue(@NotNull DeserializationContext ctx, @NotNull Class<V> clazz) throws IOException {
        return ctx.readValue(ctx.getParser(), clazz);
    }

    protected interface ObjectFieldParser {
        void parse(@NotNull DeserializationContext ctx, @NotNull String key) throws IOException;
    }

    protected void readObjectFields(@NotNull DeserializationContext ctx, @NotNull ObjectFieldParser parser) throws IOException {
        expectToken(ctx, JsonToken.START_OBJECT);
        JsonParser p = ctx.getParser();
        while (p.nextToken() != JsonToken.END_OBJECT) {
            expectToken(ctx, JsonToken.FIELD_NAME);
            String propertyName = p.currentName();
            if (p.nextToken() != JsonToken.VALUE_NULL) {
                parser.parse(ctx, propertyName);
            }
        }
    }

    protected interface ArrayElementParser<V> {
        V parse(@NotNull DeserializationContext ctx) throws IOException;
    }

    protected <V> void readArray(@NotNull DeserializationContext ctx, @NotNull List<V> out, @NotNull ArrayElementParser<V> parser) throws IOException {
        expectToken(ctx, JsonToken.START_ARRAY);
        JsonParser p = ctx.getParser();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            out.add(parser.parse(ctx));
        }
    }
}
