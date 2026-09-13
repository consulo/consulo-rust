package org.rust.lang.core.macros.proc;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.tt.FlatTree;
import org.rust.lang.core.macros.tt.FlatTreeJsonSerializer;
import org.rust.util.RsJacksonSerializer;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
public class RequestJsonSerializer extends RsJacksonSerializer<Request> {

    public RequestJsonSerializer() {
        super(Request.class);
    }

    @Override
    public void serialize(Request request, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (request instanceof Request.ExpandMacro) {
            Request.ExpandMacro expandMacro = (Request.ExpandMacro) request;
            gen.writeStartObject();
            gen.writeFieldName("ExpandMacro");
            gen.writeStartObject();
            gen.writeFieldName("macro_body");
            FlatTreeJsonSerializer.INSTANCE.serialize(expandMacro.getMacroBody(), gen, provider);
            gen.writeStringField("macro_name", expandMacro.getMacroName());
            if (expandMacro.getAttributes() != null) {
                gen.writeFieldName("attributes");
                FlatTreeJsonSerializer.INSTANCE.serialize(expandMacro.getAttributes(), gen, provider);
            } else {
                gen.writeNullField("attributes");
            }
            gen.writeStringField("lib", expandMacro.getLib());
            gen.writeFieldName("env");
            gen.writeStartArray();
            for (List<String> list : expandMacro.getEnv()) {
                gen.writeStartArray();
                for (String s : list) {
                    gen.writeString(s);
                }
                gen.writeEndArray();
            }
            gen.writeEndArray();
            if (expandMacro.getCurrentDir() != null) {
                gen.writeStringField("current_dir", expandMacro.getCurrentDir());
            } else {
                gen.writeNullField("current_dir");
            }
            gen.writeEndObject();
            gen.writeEndObject();
        } else if (request instanceof Request.ApiVersionCheck) {
            gen.writeStartObject();
            gen.writeFieldName("ApiVersionCheck");
            gen.writeStartObject();
            gen.writeEndObject();
            gen.writeEndObject();
        }
    }
}
