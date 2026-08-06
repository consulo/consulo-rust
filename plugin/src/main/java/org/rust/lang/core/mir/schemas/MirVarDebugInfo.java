/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;
import java.util.Objects;

public final class MirVarDebugInfo {
    @Nonnull
    private final String name;
    @Nonnull
    private final MirSourceInfo source;
    @Nonnull
    private final Contents contents;
    /**
     * When present, indicates what argument number this variable is in the function that it
     * originated from (starting from 1).
     */
    @Nullable
    private final Integer argumentIndex;

    public MirVarDebugInfo(
        @Nonnull String name,
        @Nonnull MirSourceInfo source,
        @Nonnull Contents contents,
        @Nullable Integer argumentIndex
    ) {
        this.name = name;
        this.source = source;
        this.contents = contents;
        this.argumentIndex = argumentIndex;
    }

    public MirVarDebugInfo(
        @Nonnull String name,
        @Nonnull MirSourceInfo source,
        @Nonnull Contents contents
    ) {
        this(name, source, contents, null);
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public MirSourceInfo getSource() {
        return source;
    }

    @Nonnull
    public Contents getContents() {
        return contents;
    }

    @Nullable
    public Integer getArgumentIndex() {
        return argumentIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MirVarDebugInfo that = (MirVarDebugInfo) o;
        return Objects.equals(name, that.name)
            && Objects.equals(source, that.source)
            && Objects.equals(contents, that.contents)
            && Objects.equals(argumentIndex, that.argumentIndex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, source, contents, argumentIndex);
    }

    @Override
    public String toString() {
        return "MirVarDebugInfo(name=" + name + ", source=" + source + ", contents=" + contents + ", argumentIndex=" + argumentIndex + ")";
    }

    public abstract static class Contents {
        private Contents() {
        }

        public static final class Place extends Contents {
            @Nonnull
            private final MirPlace place;

            public Place(@Nonnull MirPlace place) {
                this.place = place;
            }

            @Nonnull
            public MirPlace getPlace() {
                return place;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Place place1 = (Place) o;
                return Objects.equals(place, place1.place);
            }

            @Override
            public int hashCode() {
                return Objects.hash(place);
            }

            @Override
            public String toString() {
                return "Place(place=" + place + ")";
            }
        }

        public static final class Constant extends Contents {
            @Nonnull
            private final MirConstant constant;

            public Constant(@Nonnull MirConstant constant) {
                this.constant = constant;
            }

            @Nonnull
            public MirConstant getConstant() {
                return constant;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Constant constant1 = (Constant) o;
                return Objects.equals(constant, constant1.constant);
            }

            @Override
            public int hashCode() {
                return Objects.hash(constant);
            }

            @Override
            public String toString() {
                return "Constant(constant=" + constant + ")";
            }
        }

        public static final class Composite extends Contents {
            @Nonnull
            private final Ty ty;
            @Nonnull
            private final List<Fragment> fragments;

            public Composite(@Nonnull Ty ty, @Nonnull List<Fragment> fragments) {
                this.ty = ty;
                this.fragments = fragments;
            }

            @Nonnull
            public Ty getTy() {
                return ty;
            }

            @Nonnull
            public List<Fragment> getFragments() {
                return fragments;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Composite composite = (Composite) o;
                return Objects.equals(ty, composite.ty) && Objects.equals(fragments, composite.fragments);
            }

            @Override
            public int hashCode() {
                return Objects.hash(ty, fragments);
            }

            @Override
            public String toString() {
                return "Composite(ty=" + ty + ", fragments=" + fragments + ")";
            }
        }
    }

    public static final class Fragment {
        @Nonnull
        private final List<MirProjectionElem<Ty>> projection;
        @Nonnull
        private final MirPlace contents;

        public Fragment(@Nonnull List<MirProjectionElem<Ty>> projection, @Nonnull MirPlace contents) {
            this.projection = projection;
            this.contents = contents;
        }

        @Nonnull
        public List<MirProjectionElem<Ty>> getProjection() {
            return projection;
        }

        @Nonnull
        public MirPlace getContents() {
            return contents;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Fragment fragment = (Fragment) o;
            return Objects.equals(projection, fragment.projection) && Objects.equals(contents, fragment.contents);
        }

        @Override
        public int hashCode() {
            return Objects.hash(projection, contents);
        }

        @Override
        public String toString() {
            return "Fragment(projection=" + projection + ", contents=" + contents + ")";
        }
    }
}
