/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import com.intellij.util.SmartList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
// import removed
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.ty.Ty;

import java.util.*;

/**
 * Represents the source of a trait implementation.
 */
public abstract class TraitImplSource {

    @NotNull
    public abstract RsTraitOrImpl getValue();

    @Nullable
    public BoundElement<RsTraitItem> getImplementedTrait() {
        return RsTraitOrImplUtil.getImplementedTrait(getValue());
    }

    @NotNull
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
        @NotNull
        private final RsCachedImplItem cachedImpl;

        public ExplicitImpl(@NotNull RsCachedImplItem cachedImpl) {
            this.cachedImpl = cachedImpl;
        }

        @NotNull
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

        @NotNull
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
        @NotNull
        private final RsTraitItem value;
        private final boolean isInherent;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public TraitBound(@NotNull RsTraitItem value, boolean isInherent) {
            this.value = value;
            this.isInherent = isInherent;
        }

        @NotNull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @Override
        public boolean isInherent() {
            return isInherent;
        }

        @NotNull
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
        @NotNull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public ProjectionBound(@NotNull RsTraitItem value) {
            this.value = value;
        }

        @NotNull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @NotNull
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
        @NotNull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public Derived(@NotNull RsTraitItem value) {
            this.value = value;
        }

        @NotNull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @NotNull
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
        @NotNull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public Object(@NotNull RsTraitItem value) {
            this.value = value;
        }

        @NotNull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @Override
        public boolean isInherent() {
            return true;
        }

        @NotNull
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
        @NotNull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public Collapsed(@NotNull RsTraitItem value) {
            this.value = value;
        }

        @NotNull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @NotNull
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
        @NotNull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public Trait(@NotNull RsTraitItem value) {
            this.value = value;
        }

        @NotNull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @NotNull
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
        @NotNull
        private final RsTraitItem value;
        private volatile Map<String, List<RsAbstractable>> implAndTraitExpandedMembers;

        public Builtin(@NotNull RsTraitItem value) {
            this.value = value;
        }

        @NotNull
        @Override
        public RsTraitItem getValue() {
            return value;
        }

        @NotNull
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

    @NotNull
    public static Map<String, List<RsAbstractable>> collectTraitMembers(@NotNull RsTraitItem trait) {
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
