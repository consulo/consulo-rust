/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class LifetimeName {
    private LifetimeName() {
    }

    public abstract boolean isElided();

    /** User-given names or fresh (synthetic) names. */
    public static final class Parameter extends LifetimeName {
        @NotNull
        private final String myName;

        public Parameter(@NotNull String name) {
            myName = name;
        }

        @NotNull
        public String getName() {
            return myName;
        }

        @Override
        public boolean isElided() {
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Parameter)) return false;
            return myName.equals(((Parameter) o).myName);
        }

        @Override
        public int hashCode() {
            return myName.hashCode();
        }

        @Override
        public String toString() {
            return "LifetimeName.Parameter(" + myName + ")";
        }
    }

    /** User typed nothing. e.g. the lifetime in {@code &u32}. */
    public static final class Implicit extends LifetimeName {
        public static final Implicit INSTANCE = new Implicit();

        private Implicit() {
        }

        @Override
        public boolean isElided() {
            return true;
        }

        @Override
        public String toString() {
            return "LifetimeName.Implicit";
        }
    }

    /** User typed {@code '_}. */
    public static final class Underscore extends LifetimeName {
        public static final Underscore INSTANCE = new Underscore();

        private Underscore() {
        }

        @Override
        public boolean isElided() {
            return true;
        }

        @Override
        public String toString() {
            return "LifetimeName.Underscore";
        }
    }

    /** User wrote {@code 'static}. */
    public static final class Static extends LifetimeName {
        public static final Static INSTANCE = new Static();

        private Static() {
        }

        @Override
        public boolean isElided() {
            return false;
        }

        @Override
        public String toString() {
            return "LifetimeName.Static";
        }
    }
}
