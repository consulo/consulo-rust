/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import consulo.util.collection.SmartList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
// import removed
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.ty.Ty;

import java.util.*;
import org.rust.lang.core.psi.ext.impl.*;

/**
 * Represents the source of a trait implementation.
 */
public abstract class TraitImplSource {

    @Nonnull
    public abstract RsTraitOrImpl getValue();

    @Nullable
    public BoundElement<RsTraitItem> getImplementedTrait() {
        return RsTraitOrImplUtil.getImplementedTrait(getValue());
    }

    @Nonnull
    public abstract Map<String, List<RsAbstractable>> getImplAndTraitExpandedMembers();

    public boolean isInherent() {
        return false;
    }

    @Nullable
    public RsTraitItem getRequiredTraitInScope() {
        if (isInherent()) return null;
        if (this instanceof ExplicitImpl) {
            BoundElement<RsTraitItem> trait = getImplementedTrait();
            return trait != null ? (RsTraitItem) trait.getElement() : null;
        }
        return (RsTraitItem) getValue();
    }

    @Nullable
    public RsImplItem getImpl() {
        if (this instanceof ExplicitImpl) {
            return ((ExplicitImpl) this).getValue();
        }
        return null;
    }

    // --- Subclasses ---

    /** An impl block, directly defined in the code */
    public static final class ExplicitImpl extends TraitImplSource {
        @Nonnull
        private final RsCachedImplItem cachedImpl;

        public ExplicitImpl(@Nonnull RsCachedImplItem cachedImpl) {
            this.cachedImpl = cachedImpl;
        }

        @Nonnull
        @Override
        public RsImplItem getValue() {
            return cachedImpl.getImpl();
        }

        @Override
        public boolean isInherent() {
            return cachedImpl.isInherent();
        }

        @Nullable
        @Override
        public BoundElement<RsTraitItem> getImplementedTrait() {
            return cachedImpl.getImplementedTrait();
        }

        @Nonnull
        @Override
        public Map<String, List<RsAbstractable>> getImplAndTraitExpandedMembers() {
            return cachedImpl.getImplAndTraitExpandedMembers();
        }

        @Nullable
        public Ty getType() {
            return cachedImpl.getType();
        }

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (!(o instanceof ExplicitImpl)) return false;
            return cachedImpl.equals(((ExplicitImpl) o).cachedImpl);
        }

        @Override
        public int hashCode() {
            return cachedImpl.hashCode();
        }
    }

    /** T: Trait */
    public static final class TraitBound extends TraitImplSource {
        @Nonnull
        private final RsTraitItem value;
        private final boolean isInherent;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public TraitBound(@Nonnull RsTraitItem value, boolean isInherent) {
            this.value = value;
            this.isInherent = isInherent;
        }

        @Nonnull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @Override
        public boolean isInherent() {
            return isInherent;
        }

        @Nonnull
        @Override
        public Map<String, List<RsAbstractable>> getImplAndTraitExpandedMembers() {
            if (implAndTraitExpandedMembers == null) {
                implAndTraitExpandedMembers = collectTraitMembers(value);
            }
            return implAndTraitExpandedMembers;
        }

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (!(o instanceof TraitBound)) return false;
            TraitBound that = (TraitBound) o;
            return isInherent == that.isInherent && value.equals(that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, isInherent);
        }
    }

    /**
     * Like TraitBound, but this is a bound for an associated type projection defined at the trait
     * of the associated type.
     */
    public static final class ProjectionBound extends TraitImplSource {
        @Nonnull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public ProjectionBound(@Nonnull RsTraitItem value) {
            this.value = value;
        }

        @Nonnull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @Nonnull
        @Override
        public Map<String, List<RsAbstractable>> getImplAndTraitExpandedMembers() {
            if (implAndTraitExpandedMembers == null) {
                implAndTraitExpandedMembers = collectTraitMembers(value);
            }
            return implAndTraitExpandedMembers;
        }

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (!(o instanceof ProjectionBound)) return false;
            return value.equals(((ProjectionBound) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    /** Trait is implemented for item via {@code #[derive]} attribute. */
    public static final class Derived extends TraitImplSource {
        @Nonnull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public Derived(@Nonnull RsTraitItem value) {
            this.value = value;
        }

        @Nonnull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @Nonnull
        @Override
        public Map<String, List<RsAbstractable>> getImplAndTraitExpandedMembers() {
            if (implAndTraitExpandedMembers == null) {
                implAndTraitExpandedMembers = collectTraitMembers(value);
            }
            return implAndTraitExpandedMembers;
        }

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (!(o instanceof Derived)) return false;
            return value.equals(((Derived) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    /** dyn/impl Trait or a closure */
    public static final class Object extends TraitImplSource {
        @Nonnull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public Object(@Nonnull RsTraitItem value) {
            this.value = value;
        }

        @Nonnull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @Override
        public boolean isInherent() {
            return true;
        }

        @Nonnull
        @Override
        public Map<String, List<RsAbstractable>> getImplAndTraitExpandedMembers() {
            if (implAndTraitExpandedMembers == null) {
                implAndTraitExpandedMembers = collectTraitMembers(value);
            }
            return implAndTraitExpandedMembers;
        }

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (!(o instanceof Object)) return false;
            return value.equals(((Object) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    /**
     * Used only as a result of method pick. It means that method is resolved to multiple impls of the same trait
     * (with different type parameter values), so we collapsed all impls to that trait.
     */
    public static final class Collapsed extends TraitImplSource {
        @Nonnull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public Collapsed(@Nonnull RsTraitItem value) {
            this.value = value;
        }

        @Nonnull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @Nonnull
        @Override
        public Map<String, List<RsAbstractable>> getImplAndTraitExpandedMembers() {
            if (implAndTraitExpandedMembers == null) {
                implAndTraitExpandedMembers = collectTraitMembers(value);
            }
            return implAndTraitExpandedMembers;
        }

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (!(o instanceof Collapsed)) return false;
            return value.equals(((Collapsed) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    /**
     * A trait is directly referenced in UFCS path {@code TraitName::foo}, an impl should be selected
     * during type inference.
     */
    public static final class Trait extends TraitImplSource {
        @Nonnull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public Trait(@Nonnull RsTraitItem value) {
            this.value = value;
        }

        @Nonnull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @Nonnull
        @Override
        public Map<String, List<RsAbstractable>> getImplAndTraitExpandedMembers() {
            if (implAndTraitExpandedMembers == null) {
                implAndTraitExpandedMembers = collectTraitMembers(value);
            }
            return implAndTraitExpandedMembers;
        }

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (!(o instanceof Trait)) return false;
            return value.equals(((Trait) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    /** A built-in trait impl, like {@code Clone} impl for tuples */
    public static final class Builtin extends TraitImplSource {
        @Nonnull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public Builtin(@Nonnull RsTraitItem value) {
            this.value = value;
        }

        @Nonnull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @Nonnull
        @Override
        public Map<String, List<RsAbstractable>> getImplAndTraitExpandedMembers() {
            if (implAndTraitExpandedMembers == null) {
                implAndTraitExpandedMembers = collectTraitMembers(value);
            }
            return implAndTraitExpandedMembers;
        }

        @Override
        public boolean equals(java.lang.Object o) {
            if (this == o) return true;
            if (!(o instanceof Builtin)) return false;
            return value.equals(((Builtin) o).value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }

    // --- Static utility methods ---

    @Nonnull
    public static Map<String, List<RsAbstractable>> collectTraitMembers(@Nonnull RsTraitItem trait) {
        RsMembers members = trait.getMembers();
        if (members == null) return Collections.emptyMap();
        List<RsAbstractable> expandedMembers = RsMembersUtil.getExpandedMembers(members);
        Map<String, List<RsAbstractable>> membersMap = new Object2ObjectOpenHashMap<>(expandedMembers.size());
        for (RsAbstractable member : expandedMembers) {
            String name = member.getName();
            if (name == null) continue;
            membersMap.computeIfAbsent(name, k -> new SmartList<>()).add(member);
        }
        return membersMap;
    }
}
