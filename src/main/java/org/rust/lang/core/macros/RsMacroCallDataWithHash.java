/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.stdext.HashCode;

import java.util.Map;
import java.util.stream.Collectors;

public class RsMacroCallDataWithHash {
    private final RsMacroCallData myData;
    private final HashCode myBodyHash;

    public RsMacroCallDataWithHash(@NotNull RsMacroCallData data, @Nullable HashCode bodyHash) {
        myData = data;
        myBodyHash = bodyHash;
    }

    @NotNull
    public RsMacroCallData getData() {
        return myData;
    }

    @Nullable
    public HashCode getBodyHash() {
        return myBodyHash;
    }

    @Nullable
    public HashCode hashWithEnv() {
        if (myBodyHash == null) return null;
        String env = myData.getEnv()
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(";"));
        HashCode envHash = HashCode.compute(env);
        return HashCode.mix(myBodyHash, envHash);
    }
}
