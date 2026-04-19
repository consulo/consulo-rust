/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import java.util.Objects;

// TODO: int -> uint128, size -> non zero u8
public final class MirScalarInt {
    private final long data;
    private final byte size;

    public MirScalarInt(long data, byte size) {
        this.data = data;
        this.size = size;
    }

    public long getData() {
        return data;
    }

    public byte getSize() {
        return size;
    }

    // TODO there are some checks, maybe it'll be needed later
    public long toBits() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MirScalarInt that = (MirScalarInt) o;
        return data == that.data && size == that.size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, size);
    }

    @Override
    public String toString() {
        return "MirScalarInt(data=" + data + ", size=" + size + ")";
    }
}
