/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsSelfParameter;


public abstract class LocalVar {
    @Nonnull
    public abstract String getName();

    private LocalVar() {
    }

    public static class FromPatBinding extends LocalVar {
        @Nonnull
        public final RsPatBinding pat;

        public FromPatBinding(@Nonnull RsPatBinding pat) {
            this.pat = pat;
        }

        @Nonnull
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
        @Nonnull
        public final RsSelfParameter self;

        public FromSelfParameter(@Nonnull RsSelfParameter self) {
            this.self = self;
        }

        @Nonnull
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

    @Nonnull
    public static LocalVar from(@Nonnull RsPatBinding pat) {
        return new FromPatBinding(pat);
    }

    @Nonnull
    public static LocalVar from(@Nonnull RsSelfParameter self) {
        return new FromSelfParameter(self);
    }
}
