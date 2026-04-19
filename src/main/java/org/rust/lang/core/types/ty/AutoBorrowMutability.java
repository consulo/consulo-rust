/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

public abstract class AutoBorrowMutability {
    private AutoBorrowMutability() {}

    public static final class Mutable extends AutoBorrowMutability {
        private final boolean myAllowTwoPhaseBorrow;

        public Mutable(boolean allowTwoPhaseBorrow) {
            myAllowTwoPhaseBorrow = allowTwoPhaseBorrow;
        }

        public boolean isAllowTwoPhaseBorrow() {
            return myAllowTwoPhaseBorrow;
        }
    }

    public static final AutoBorrowMutability Immutable = new AutoBorrowMutability() {
        @Override
        public String toString() {
            return "Immutable";
        }
    };
}
