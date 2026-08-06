/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;

public abstract class RsVisibility {

    private RsVisibility() {
    }

    public static final class Private extends RsVisibility {
        @Nonnull
        public static final Private INSTANCE = new Private();

        private Private() {
        }

        @Override
        public String toString() {
            return "RsVisibility.Private";
        }
    }

    public static final class Public extends RsVisibility {
        @Nonnull
        public static final Public INSTANCE = new Public();

        private Public() {
        }

        @Override
        public String toString() {
            return "RsVisibility.Public";
        }
    }

    public static final class Restricted extends RsVisibility {
        @Nonnull
        private final RsMod myInMod;

        public Restricted(@Nonnull RsMod inMod) {
            myInMod = inMod;
        }

        @Nonnull
        public RsMod getInMod() {
            return myInMod;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Restricted)) return false;
            return myInMod.equals(((Restricted) o).myInMod);
        }

        @Override
        public int hashCode() {
            return myInMod.hashCode();
        }

        @Override
        public String toString() {
            return "RsVisibility.Restricted(inMod=" + myInMod + ")";
        }
    }
}
