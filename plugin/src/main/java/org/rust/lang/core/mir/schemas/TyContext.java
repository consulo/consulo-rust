/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/** Extra information passed to visitTy and friends to give context about where the type etc appears. */
public abstract class TyContext {
    private TyContext() {
    }

    public static final class LocalDecl extends TyContext {
        /** The index of the local variable we are visiting. */
        @Nonnull
        private final MirLocal local;
        /** The source location where this local variable was declared. */
        @Nonnull
        private final MirSourceInfo sourceInfo;

        public LocalDecl(@Nonnull MirLocal local, @Nonnull MirSourceInfo sourceInfo) {
            this.local = local;
            this.sourceInfo = sourceInfo;
        }

        @Nonnull
        public MirLocal getLocal() {
            return local;
        }

        @Nonnull
        public MirSourceInfo getSourceInfo() {
            return sourceInfo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LocalDecl localDecl = (LocalDecl) o;
            return Objects.equals(local, localDecl.local) && Objects.equals(sourceInfo, localDecl.sourceInfo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(local, sourceInfo);
        }

        @Override
        public String toString() {
            return "LocalDecl(local=" + local + ", sourceInfo=" + sourceInfo + ")";
        }
    }

    /** A type found at some location. */
    public static final class Location extends TyContext {
        @Nonnull
        private final MirLocation location;

        public Location(@Nonnull MirLocation location) {
            this.location = location;
        }

        @Nonnull
        public MirLocation getLocation() {
            return location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Location location1 = (Location) o;
            return Objects.equals(location, location1.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location);
        }

        @Override
        public String toString() {
            return "Location(location=" + location + ")";
        }
    }
}
