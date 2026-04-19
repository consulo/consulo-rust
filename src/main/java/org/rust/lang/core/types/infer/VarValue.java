/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class VarValue<V> implements NodeOrValue {
    @Nullable
    private final V myValue;
    private final int myRank;

    public VarValue(@Nullable V value, int rank) {
        myValue = value;
        myRank = rank;
    }

    @Nullable
    public V getValue() {
        return myValue;
    }

    public int getRank() {
        return myRank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VarValue<?> varValue = (VarValue<?>) o;
        return myRank == varValue.myRank && Objects.equals(myValue, varValue.myValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myValue, myRank);
    }
}
