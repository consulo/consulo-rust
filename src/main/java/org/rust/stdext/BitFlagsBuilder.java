/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

public class BitFlagsBuilder {
    private final Limit myLimit;
    private int myCounter;

    public BitFlagsBuilder(Limit limit) {
        this.myLimit = limit;
        this.myCounter = 0;
    }

    public BitFlagsBuilder(BitFlagsBuilder prevBuilder, Limit limit) {
        this.myLimit = limit;
        this.myCounter = prevBuilder.myCounter;
    }

    public int nextBitMask() {
        int nextBit = myCounter++;
        if (nextBit == myLimit.getBits()) {
            throw new IllegalStateException("Bitmask index out of " + myLimit + " limit!");
        }
        return CollectionsUtil.makeBitMask(nextBit);
    }

    public enum Limit {
        BYTE(8),
        INT(32);

        private final int myBits;

        Limit(int bits) {
            this.myBits = bits;
        }

        public int getBits() {
            return myBits;
        }
    }
}
