/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsSelfParameter;

import java.util.Objects;

public abstract class LocalVar {
    @NotNull
    public abstract String getName();

    private LocalVar() {
    }

    public static class FromPatBinding extends LocalVar {
        @NotNull
        public final RsPatBinding pat;

        public FromPatBinding(@NotNull RsPatBinding pat) {
            this.pat = pat;
        }

        @NotNull
        @Override
        public String getName() {
            String name = pat.getName();
            if (name == null) throw new NullPointerException("pat.getName() returned null");
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FromPatBinding)) return false;
            return pat.equals(((FromPatBinding) o).pat);
        }

        @Override
        public int hashCode() {
            return pat.hashCode();
        }
    }

    public static class FromSelfParameter extends LocalVar {
        @NotNull
        public final RsSelfParameter self;

        public FromSelfParameter(@NotNull RsSelfParameter self) {
            this.self = self;
        }

        @NotNull
        @Override
        public String getName() {
            return "self";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FromSelfParameter)) return false;
            return self.equals(((FromSelfParameter) o).self);
        }

        @Override
        public int hashCode() {
            return self.hashCode();
        }
    }

    @NotNull
    public static LocalVar from(@NotNull RsPatBinding pat) {
        return new FromPatBinding(pat);
    }

    @NotNull
    public static LocalVar from(@NotNull RsSelfParameter self) {
        return new FromSelfParameter(self);
    }
}
