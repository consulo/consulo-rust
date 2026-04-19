/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.tt.FlatTree;
import org.rust.lang.core.macros.tt.FlatTreeJsonSerializer;
import org.rust.util.RsJacksonSerializer;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Represents a request to be sent to the proc macro expander process.
 */
public abstract class Request {

    private Request() {}

    public static final class ExpandMacro extends Request {
        @NotNull
        private final FlatTree myMacroBody;
        @NotNull
        private final String myMacroName;
        @Nullable
        private final FlatTree myAttributes;
        @NotNull
        private final String myLib;
        @NotNull
        private final List<List<String>> myEnv;
        @Nullable
        private final String myCurrentDir;

        public ExpandMacro(
            @NotNull FlatTree macroBody,
            @NotNull String macroName,
            @Nullable FlatTree attributes,
            @NotNull String lib,
            @NotNull List<List<String>> env,
            @Nullable String currentDir
        ) {
            myMacroBody = macroBody;
            myMacroName = macroName;
            myAttributes = attributes;
            myLib = lib;
            myEnv = env;
            myCurrentDir = currentDir;
        }

        @NotNull
        public FlatTree getMacroBody() {
            return myMacroBody;
        }

        @NotNull
        public String getMacroName() {
            return myMacroName;
        }

        @Nullable
        public FlatTree getAttributes() {
            return myAttributes;
        }

        @NotNull
        public String getLib() {
            return myLib;
        }

        @NotNull
        public List<List<String>> getEnv() {
            return myEnv;
        }

        @Nullable
        public String getCurrentDir() {
            return myCurrentDir;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExpandMacro)) return false;
            ExpandMacro that = (ExpandMacro) o;
            return Objects.equals(myMacroBody, that.myMacroBody)
                && Objects.equals(myMacroName, that.myMacroName)
                && Objects.equals(myAttributes, that.myAttributes)
                && Objects.equals(myLib, that.myLib)
                && Objects.equals(myEnv, that.myEnv)
                && Objects.equals(myCurrentDir, that.myCurrentDir);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myMacroBody, myMacroName, myAttributes, myLib, myEnv, myCurrentDir);
        }
    }

    public static final ApiVersionCheck API_VERSION_CHECK = new ApiVersionCheck();

    public static final class ApiVersionCheck extends Request {
        private ApiVersionCheck() {}
    }
}

class RequestJsonSerializer extends RsJacksonSerializer<Request> {

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
