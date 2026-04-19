/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TokenMap {
    @NotNull
    private final List<TokenMetadata> myMap;

    public TokenMap(@NotNull List<TokenMetadata> map) {
        myMap = map;
    }

    @NotNull
    public List<TokenMetadata> getMap() {
        return myMap;
    }

    @Nullable
    public TokenMetadata get(int id) {
        if (id >= 0 && id < myMap.size()) {
            return myMap.get(id);
        }
        return null;
    }

    @NotNull
    public TokenMap merge(@NotNull TokenMap other) {
        List<TokenMetadata> merged = new ArrayList<>(myMap.size() + other.myMap.size());
        merged.addAll(myMap);
        merged.addAll(other.myMap);
        return new TokenMap(merged);
    }
}
