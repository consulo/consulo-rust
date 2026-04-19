/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BuildMessages {

    private final Map<String, List<RustcMessage.CompilerMessage>> messages;
    private final boolean isSuccessful;

    public BuildMessages(Map<String, List<RustcMessage.CompilerMessage>> messages, boolean isSuccessful) {
        this.messages = messages;
        this.isSuccessful = isSuccessful;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }

    public List<RustcMessage.CompilerMessage> get(String packageId) {
        List<RustcMessage.CompilerMessage> result = messages.get(packageId);
        return result != null ? result : Collections.emptyList();
    }

    public BuildMessages replacePaths(Function<String, String> replacer) {
        Map<String, List<RustcMessage.CompilerMessage>> newMessages = messages.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().map(message -> {
                    if (message instanceof RustcMessage.BuildScriptMessage) {
                        RustcMessage.BuildScriptMessage bsm = (RustcMessage.BuildScriptMessage) message;
                        String outDir = bsm.getOutDir();
                        if (outDir != null) {
                            return bsm.withOutDir(replacer.apply(outDir));
                        }
                    }
                    return message;
                }).collect(Collectors.toList())
            ));
        return new BuildMessages(newMessages, isSuccessful);
    }

    public static final BuildMessages DEFAULT = new BuildMessages(Collections.emptyMap(), true);
    public static final BuildMessages FAILED = new BuildMessages(Collections.emptyMap(), false);
}
