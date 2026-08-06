/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TokenMap {
    @Nonnull
    private final List<TokenMetadata> myMap;

    public TokenMap(@Nonnull List<TokenMetadata> map) {
        myMap = map;
    }

    @Nonnull
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

    @Nonnull
    public TokenMap merge(@Nonnull TokenMap other) {
        List<TokenMetadata> merged = new ArrayList<>(myMap.size() + other.myMap.size());
        merged.addAll(myMap);
        merged.addAll(other.myMap);
        return new TokenMap(merged);
    }
}
