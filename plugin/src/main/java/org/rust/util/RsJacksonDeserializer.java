/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class RsJacksonDeserializer<T> extends StdDeserializer<T> {
    protected RsJacksonDeserializer(@Nonnull Class<T> vc) {
        super(vc);
    }

    protected void expectToken(@Nonnull DeserializationContext ctx, @Nonnull JsonToken expectedToken) throws IOException {
        if (ctx.getParser().currentToken() != expectedToken) {
            ctx.reportWrongTokenException(handledType(), expectedToken, null);
        }
    }

    protected void expectNextToken(@Nonnull DeserializationContext ctx, @Nonnull JsonToken expectedToken) throws IOException {
        if (ctx.getParser().nextToken() != expectedToken) {
            ctx.reportWrongTokenException(handledType(), expectedToken, null);
        }
    }

    @Nonnull
    protected String expectNextFieldName(@Nonnull DeserializationContext ctx) throws IOException {
        String name = ctx.getParser().nextFieldName();
        if (name == null) {
            ctx.reportWrongTokenException(handledType(), JsonToken.FIELD_NAME, null);
            throw new IllegalStateException("unreachable");
        }
        return name;
    }

    @Nonnull
    protected String readString(@Nonnull DeserializationContext ctx) throws IOException {
        return _parseString(ctx.getParser(), ctx);
    }

    protected long readLong(@Nonnull DeserializationContext ctx) throws IOException {
        return _parseLongPrimitive(ctx.getParser(), ctx);
    }

    protected <V> V readValue(@Nonnull DeserializationContext ctx, @Nonnull Class<V> clazz) throws IOException {
        return ctx.readValue(ctx.getParser(), clazz);
    }

    protected interface ObjectFieldParser {
        void parse(@Nonnull DeserializationContext ctx, @Nonnull String key) throws IOException;
    }

    protected void readObjectFields(@Nonnull DeserializationContext ctx, @Nonnull ObjectFieldParser parser) throws IOException {
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
        V parse(@Nonnull DeserializationContext ctx) throws IOException;
    }

    protected <V> void readArray(@Nonnull DeserializationContext ctx, @Nonnull List<V> out, @Nonnull ArrayElementParser<V> parser) throws IOException {
        expectToken(ctx, JsonToken.START_ARRAY);
        JsonParser p = ctx.getParser();
        while (p.nextToken() != JsonToken.END_ARRAY) {
            out.add(parser.parse(ctx));
        }
    }
}
