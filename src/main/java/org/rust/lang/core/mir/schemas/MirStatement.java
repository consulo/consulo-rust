/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Objects;

public abstract class MirStatement {
    @NotNull
    private final MirSourceInfo source;

    protected MirStatement(@NotNull MirSourceInfo source) {
        this.source = source;
    }

    @NotNull
    public MirSourceInfo getSource() {
        return source;
    }

    public static final class Assign extends MirStatement {
        @NotNull
        private final MirPlace place;
        @NotNull
        private final MirRvalue rvalue;

        public Assign(@NotNull MirPlace place, @NotNull MirRvalue rvalue, @NotNull MirSourceInfo source) {
            super(source);
            this.place = place;
            this.rvalue = rvalue;
        }

        @NotNull
        public MirPlace getPlace() {
            return place;
        }

        @NotNull
        public MirRvalue getRvalue() {
            return rvalue;
        }

        @Override
        public String toString() {
            return "Assign(place=" + place + ", rvalue=" + rvalue + ")";
        }
    }

    public static final class StorageDead extends MirStatement {
        @NotNull
        private final MirLocal local;

        public StorageDead(@NotNull MirLocal local, @NotNull MirSourceInfo source) {
            super(source);
            this.local = local;
        }

        @NotNull
        public MirLocal getLocal() {
            return local;
        }

        @Override
        public String toString() {
            return "StorageDead(local=" + local + ")";
        }
    }

    public static final class StorageLive extends MirStatement {
        @NotNull
        private final MirLocal local;

        public StorageLive(@NotNull MirLocal local, @NotNull MirSourceInfo source) {
            super(source);
            this.local = local;
        }

        @NotNull
        public MirLocal getLocal() {
            return local;
        }

        @Override
        public String toString() {
            return "StorageLive(local=" + local + ")";
        }
    }

    public static final class FakeRead extends MirStatement {
        @NotNull
        private final Cause cause;
        @NotNull
        private final MirPlace place;

        public FakeRead(@NotNull Cause cause, @NotNull MirPlace place, @NotNull MirSourceInfo source) {
            super(source);
            this.cause = cause;
            this.place = place;
        }

        @NotNull
        public Cause getCause() {
            return cause;
        }

        @NotNull
        public MirPlace getPlace() {
            return place;
        }

        public abstract static class Cause {
            private Cause() {
            }

            public static final class ForMatchedPlace extends Cause {
                @Nullable
                private final RsElement element;

                public ForMatchedPlace(@Nullable RsElement element) {
                    this.element = element;
                }

                @Nullable
                public RsElement getElement() {
                    return element;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    ForMatchedPlace that = (ForMatchedPlace) o;
                    return Objects.equals(element, that.element);
                }

                @Override
                public int hashCode() {
                    return Objects.hash(element);
                }

                @Override
                public String toString() {
                    return "ForMatchedPlace(element=" + element + ")";
                }
            }

            public static final class ForLet extends Cause {
                @Nullable
                private final RsElement element;

                public ForLet(@Nullable RsElement element) {
                    this.element = element;
                }

                @Nullable
                public RsElement getElement() {
                    return element;
                }

                @Override
                public boolean equals(Object o) {
                    if (this == o) return true;
                    if (o == null || getClass() != o.getClass()) return false;
                    ForLet forLet = (ForLet) o;
                    return Objects.equals(element, forLet.element);
                }

                @Override
                public int hashCode() {
                    return Objects.hash(element);
                }

                @Override
                public String toString() {
                    return "ForLet(element=" + element + ")";
                }
            }

            public static final class ForIndex extends Cause {
                public static final ForIndex INSTANCE = new ForIndex();

                private ForIndex() {
                }

                @Override
                public String toString() {
                    return "ForIndex";
                }
            }
        }
    }
}
