/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;

public abstract class RsVisibility {

    private RsVisibility() {
    }

    public static final class Private extends RsVisibility {
        @NotNull
        public static final Private INSTANCE = new Private();

        private Private() {
        }

        @Override
        public String toString() {
            return "RsVisibility.Private";
        }
    }

    public static final class Public extends RsVisibility {
        @NotNull
        public static final Public INSTANCE = new Public();

        private Public() {
        }

        @Override
        public String toString() {
            return "RsVisibility.Public";
        }
    }

    public static final class Restricted extends RsVisibility {
        @NotNull
        private final RsMod myInMod;

        public Restricted(@NotNull RsMod inMod) {
            myInMod = inMod;
        }

        @NotNull
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
