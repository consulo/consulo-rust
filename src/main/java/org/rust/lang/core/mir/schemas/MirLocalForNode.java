/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class MirLocalForNode {
    private MirLocalForNode() {
    }

    public static final class One extends MirLocalForNode {
        @NotNull
        private final MirLocal local;

        public One(@NotNull MirLocal local) {
            this.local = local;
        }

        @NotNull
        public MirLocal getLocal() {
            return local;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            One one = (One) o;
            return Objects.equals(local, one.local);
        }

        @Override
        public int hashCode() {
            return Objects.hash(local);
        }

        @Override
        public String toString() {
            return "One(local=" + local + ")";
        }
    }

    public static final class ForGuard extends MirLocalForNode {
        @NotNull
        private final MirLocal refForGuard;
        @NotNull
        private final MirLocal forArmBody;

        public ForGuard(@NotNull MirLocal refForGuard, @NotNull MirLocal forArmBody) {
            this.refForGuard = refForGuard;
            this.forArmBody = forArmBody;
        }

        @NotNull
        public MirLocal getRefForGuard() {
            return refForGuard;
        }

        @NotNull
        public MirLocal getForArmBody() {
            return forArmBody;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ForGuard forGuard = (ForGuard) o;
            return Objects.equals(refForGuard, forGuard.refForGuard) && Objects.equals(forArmBody, forGuard.forArmBody);
        }

        @Override
        public int hashCode() {
            return Objects.hash(refForGuard, forArmBody);
        }

        @Override
        public String toString() {
            return "ForGuard(refForGuard=" + refForGuard + ", forArmBody=" + forArmBody + ")";
        }
    }
}
