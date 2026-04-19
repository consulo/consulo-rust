/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class ParameterLookup {

    public static class Named extends ParameterLookup {
        @NotNull
        private final String myName;

        public Named(@NotNull String name) {
            this.myName = name;
        }

        @NotNull
        public String getName() {
            return myName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Named)) return false;
            return myName.equals(((Named) o).myName);
        }

        @Override
        public int hashCode() {
            return myName.hashCode();
        }
    }

    public static class Positional extends ParameterLookup {
        private final int myPosition;

        public Positional(int position) {
            this.myPosition = position;
        }

        public int getPosition() {
            return myPosition;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Positional)) return false;
            return myPosition == ((Positional) o).myPosition;
        }

        @Override
        public int hashCode() {
            return Objects.hash(myPosition);
        }
    }
}
