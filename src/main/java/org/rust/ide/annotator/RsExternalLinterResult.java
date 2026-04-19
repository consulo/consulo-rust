/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import org.jetbrains.annotations.NotNull;
import org.rust.cargo.toolchain.impl.RustcMessage;
import org.rust.openapiext.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class RsExternalLinterResult {
    private static final Pattern MESSAGE_REGEX = Pattern.compile("\\s*\\{.*\"message\".*");

    @NotNull
    private final List<RustcMessage.CargoTopMessage> myMessages;
    private final long myExecutionTime;

    public RsExternalLinterResult(@NotNull List<String> commandOutput, long executionTime) {
        this.myExecutionTime = executionTime;
        List<RustcMessage.CargoTopMessage> messages = new ArrayList<>();
        for (String line : commandOutput) {
            if (MESSAGE_REGEX.matcher(line).matches()) {
                com.google.gson.JsonObject jsonObject = JsonUtils.tryParseJsonObject(line);
                if (jsonObject != null) {
                    RustcMessage.CargoTopMessage topMessage = RustcMessage.CargoTopMessage.fromJson(jsonObject);
                    if (topMessage != null) {
                        messages.add(topMessage);
                    }
                }
            }
        }
        this.myMessages = messages;
    }

    @NotNull
    public List<RustcMessage.CargoTopMessage> getMessages() {
        return myMessages;
    }

    public long getExecutionTime() {
        return myExecutionTime;
    }
}
