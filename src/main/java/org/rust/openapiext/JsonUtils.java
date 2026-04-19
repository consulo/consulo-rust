/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.StringReader;

public final class JsonUtils {
    public static final JsonUtils INSTANCE = new JsonUtils();

    private JsonUtils() {
    }

    @Nullable
    public static JsonObject tryParseJsonObject(@Nullable String content) {
        return tryParseJsonObject(content, true);
    }

    @Nullable
    public static JsonObject tryParseJsonObject(@Nullable String content, boolean lenient) {
        try {
            return parseJsonObject(content, lenient);
        } catch (Exception ignored) {
            return null;
        }
    }

    @NotNull
    public static JsonObject parseJsonObject(@Nullable String content) throws JsonIOException, JsonSyntaxException, IllegalStateException {
        return parseJsonObject(content, true);
    }

    @NotNull
    public static JsonObject parseJsonObject(@Nullable String content, boolean lenient) throws JsonIOException, JsonSyntaxException, IllegalStateException {
        JsonReader jsonReader = new JsonReader(new StringReader(content != null ? content : ""));
        jsonReader.setLenient(lenient);
        try {
            return JsonParser.parseReader(jsonReader).getAsJsonObject();
        } finally {
            try {
                jsonReader.close();
            } catch (Exception ignored) {
            }
        }
    }
}
