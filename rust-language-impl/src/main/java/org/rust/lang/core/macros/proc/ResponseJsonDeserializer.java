package org.rust.lang.core.macros.proc;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.tt.FlatTree;
import org.rust.lang.core.macros.tt.FlatTreeJsonDeserializer;
import org.rust.stdext.RsResult;
import org.rust.util.RsJacksonDeserializer;
import java.io.IOException;
import java.util.Objects;
public class ResponseJsonDeserializer extends RsJacksonDeserializer<Response> {

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
