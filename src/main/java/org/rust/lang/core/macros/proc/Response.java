/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.tt.FlatTree;
import org.rust.lang.core.macros.tt.FlatTreeJsonDeserializer;
import org.rust.stdext.RsResult;
import org.rust.util.RsJacksonDeserializer;

import java.io.IOException;
import java.util.Objects;

/**
 * Represents a response from the proc macro expander process.
 */
public abstract class Response {

    private Response() {}

    public static final class ExpandMacro extends Response {
        @NotNull
        private final RsResult<FlatTree, PanicMessage> myExpansion;

        public ExpandMacro(@NotNull RsResult<FlatTree, PanicMessage> expansion) {
            myExpansion = expansion;
        }

        @NotNull
        public RsResult<FlatTree, PanicMessage> getExpansion() {
            return myExpansion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExpandMacro)) return false;
            return Objects.equals(myExpansion, ((ExpandMacro) o).myExpansion);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myExpansion);
        }
    }

    public static final class ApiVersionCheck extends Response {
        private final long myVersion;

        public ApiVersionCheck(long version) {
            myVersion = version;
        }

        public long getVersion() {
            return myVersion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ApiVersionCheck)) return false;
            return myVersion == ((ApiVersionCheck) o).myVersion;
        }

        @Override
        public int hashCode() {
            return Objects.hash(myVersion);
        }
    }
}

class ResponseJsonDeserializer extends RsJacksonDeserializer<Response> {

    public ResponseJsonDeserializer() {
        super(Response.class);
    }

    @Override
    public Response deserialize(JsonParser parser, DeserializationContext context) throws IOException, JsonProcessingException {
        return readSingleFieldObject(context, key -> {
            switch (key) {
                case "ExpandMacro": {
                    RsResult<FlatTree, PanicMessage> r = readSingleFieldObject(context, key1 -> {
                        switch (key1) {
                            case "Ok":
                                return new RsResult.Ok<>(FlatTreeJsonDeserializer.INSTANCE.deserialize(parser, context));
                            case "Err":
                                return new RsResult.Err<>(new PanicMessage(readString(context)));
                            default:
                                return context.reportInputMismatch(Response.class,
                                    "Unknown variant `%s`, `Ok` or `Err` expected", key1);
                        }
                    });
                    return new Response.ExpandMacro(r);
                }
                case "ApiVersionCheck": {
                    long version = readLong(context);
                    return new Response.ApiVersionCheck(version);
                }
                default:
                    return context.reportInputMismatch(Response.class, "Unknown response kind `%s`", key);
            }
        });
    }

    @FunctionalInterface
    private interface FieldReader<T> {
        T read(String key) throws IOException;
    }

    private <T> T readSingleFieldObject(DeserializationContext context, FieldReader<T> reader) throws IOException {
        JsonParser parser = context.getParser();
        parser.nextToken(); // START_OBJECT
        parser.nextToken(); // FIELD_NAME
        String key = parser.getCurrentName();
        parser.nextToken(); // value
        T result = reader.read(key);
        parser.nextToken(); // END_OBJECT
        return result;
    }

    protected String readString(DeserializationContext context) throws IOException {
        return context.getParser().getValueAsString();
    }

    protected long readLong(DeserializationContext context) throws IOException {
        return context.getParser().getValueAsLong();
    }
}
