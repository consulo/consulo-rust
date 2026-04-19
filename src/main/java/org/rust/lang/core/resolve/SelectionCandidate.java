/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.infer.Obligation;
import org.rust.lang.core.types.infer.RsInferenceContext;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.*;
import org.rust.lang.core.types.ImplLookupUtil;
import org.rust.lang.core.types.infer.TypeInferenceUtil;
// import org.rust.lang.core.types.infer.ImplLookupUtil; // placeholder

public abstract class SelectionCandidate {

    /** @see org.rust.lang.core.types.ty.Ty#isEquivalentTo */
    public boolean isEquivalentTo(@NotNull SelectionCandidate other) {
        return this.equals(other);
    }

    // --- Subclasses ---

    public static final class BuiltinCandidate extends SelectionCandidate {
        /** {@code false} if there are no <em>further</em> obligations */
        private final boolean hasNested;

        public BuiltinCandidate(boolean hasNested) {
            this.hasNested = hasNested;
        }

        public boolean hasNested() {
            return hasNested;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BuiltinCandidate)) return false;
            return hasNested == ((BuiltinCandidate) o).hasNested;
        }

        @Override
        public int hashCode() {
            return Boolean.hashCode(hasNested);
        }
    }

    public static final class ParamCandidate extends SelectionCandidate {
        @NotNull
        private final BoundElement<RsTraitItem> bound;

        public ParamCandidate(@NotNull BoundElement<RsTraitItem> bound) {
            this.bound = bound;
        }

        @NotNull
        public BoundElement<RsTraitItem> getBound() {
            return bound;
        }

        @Override
        public boolean isEquivalentTo(@NotNull SelectionCandidate other) {
            return other instanceof ParamCandidate && bound.isEquivalentTo(((ParamCandidate) other).bound);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ParamCandidate)) return false;
            return bound.equals(((ParamCandidate) o).bound);
        }

        @Override
        public int hashCode() {
            return bound.hashCode();
        }
    }

    public static abstract class ImplCandidate extends SelectionCandidate {

        /**
         * <pre>
         * impl&lt;A, B&gt; Foo&lt;A&gt; for Bar&lt;B&gt; {}
         * |   |      |          |
         * |   |      |          formalSelfTy
         * |   |      formalTrait
         * |   subst
         * impl
         * </pre>
         */
        public static final class ExplicitImpl extends ImplCandidate {
            @NotNull
            private final RsImplItem impl;
            @NotNull
            private final Ty formalSelfTy;
            @NotNull
            private final BoundElement<RsTraitItem> formalTrait;
            private final boolean isNegativeImpl;

            public ExplicitImpl(
                @NotNull RsImplItem impl,
                @NotNull Ty formalSelfTy,
                @NotNull BoundElement<RsTraitItem> formalTrait,
                boolean isNegativeImpl
            ) {
                this.impl = impl;
                this.formalSelfTy = formalSelfTy;
                this.formalTrait = formalTrait;
                this.isNegativeImpl = isNegativeImpl;
            }

            @NotNull
            public RsImplItem getImpl() {
                return impl;
            }

            @NotNull
            public Ty getFormalSelfTy() {
                return formalSelfTy;
            }

            @NotNull
            public BoundElement<RsTraitItem> getFormalTrait() {
                return formalTrait;
            }

            public boolean isNegativeImpl() {
                return isNegativeImpl;
            }

            @Override
            public boolean isEquivalentTo(@NotNull SelectionCandidate other) {
                return other instanceof ExplicitImpl && impl == ((ExplicitImpl) other).impl;
            }

            public Triple<Substitution, TraitRef, List<Obligation>> prepareSubstAndTraitRef(
                @NotNull RsInferenceContext ctx,
                int recursionDepth
            ) {
                return ImplLookupUtil.prepareSubstAndTraitRefRaw(
                    ctx,
                    TypeInferenceUtil.getGenerics(impl),
                    TypeInferenceUtil.getConstGenerics(impl),
                    formalSelfTy,
                    formalTrait,
                    recursionDepth
                );
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof ExplicitImpl)) return false;
                ExplicitImpl that = (ExplicitImpl) o;
                return isNegativeImpl == that.isNegativeImpl &&
                    impl.equals(that.impl) &&
                    formalSelfTy.equals(that.formalSelfTy) &&
                    formalTrait.equals(that.formalTrait);
            }

            @Override
            public int hashCode() {
                return Objects.hash(impl, formalSelfTy, formalTrait, isNegativeImpl);
            }
        }

        public static final class DerivedTrait extends ImplCandidate {
            @NotNull
            private final RsTraitItem item;

            public DerivedTrait(@NotNull RsTraitItem item) {
                this.item = item;
            }

            @NotNull
            public RsTraitItem getItem() {
                return item;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof DerivedTrait)) return false;
                return item.equals(((DerivedTrait) o).item);
            }

            @Override
            public int hashCode() {
                return item.hashCode();
            }
        }
    }

    public static final class ProjectionCandidate extends SelectionCandidate {
        @NotNull
        private final BoundElement<RsTraitItem> bound;

        public ProjectionCandidate(@NotNull BoundElement<RsTraitItem> bound) {
            this.bound = bound;
        }

        @NotNull
        public BoundElement<RsTraitItem> getBound() {
            return bound;
        }

        @Override
        public boolean isEquivalentTo(@NotNull SelectionCandidate other) {
            return other instanceof ProjectionCandidate && bound.isEquivalentTo(((ProjectionCandidate) other).bound);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProjectionCandidate)) return false;
            return bound.equals(((ProjectionCandidate) o).bound);
        }

        @Override
        public int hashCode() {
            return bound.hashCode();
        }
    }

    /** @see ImplLookup */
    public static final class FnPointerCandidate extends SelectionCandidate {
        public static final FnPointerCandidate INSTANCE = new FnPointerCandidate();
        private FnPointerCandidate() {}
    }

    public static final class ObjectCandidate extends SelectionCandidate {
        public static final ObjectCandidate INSTANCE = new ObjectCandidate();
        private ObjectCandidate() {}
    }

    public static final class BuiltinUnsizeCandidate extends SelectionCandidate {
        public static final BuiltinUnsizeCandidate INSTANCE = new BuiltinUnsizeCandidate();
        private BuiltinUnsizeCandidate() {}
    }

    public static final class ConstDestructCandidate extends SelectionCandidate {
        public static final ConstDestructCandidate INSTANCE = new ConstDestructCandidate();
        private ConstDestructCandidate() {}
    }

    /**
     * Helper triple class.
     */
    public static final class Triple<A, B, C> {
        @NotNull private final A first;
        @NotNull private final B second;
        @NotNull private final C third;

        public Triple(@NotNull A first, @NotNull B second, @NotNull C third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        @NotNull
        public A getFirst() { return first; }
        @NotNull
        public B getSecond() { return second; }
        @NotNull
        public C getThird() { return third; }
    }
}
