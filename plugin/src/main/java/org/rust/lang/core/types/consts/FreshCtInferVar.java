/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.consts;

import java.util.Objects;

public class FreshCtInferVar extends Const {
    private final int myId;

    public FreshCtInferVar(int id) {
        super();
        myId = id;
    }

    public int getId() {
        return myId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FreshCtInferVar that = (FreshCtInferVar) o;
        return myId == that.myId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(myId);
    }

    @Override
    public String toString() {
        return "FreshCtInferVar(id=" + myId + ")";
    }
}
