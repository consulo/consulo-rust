/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;

import java.util.Set;

/**
 * Represents the different kinds of path resolution.
 */
public abstract class RsPathResolveKind {

    private RsPathResolveKind() {
    }

    /** A path consisting of a single identifier, e.g. {@code foo} */
    public static final class UnqualifiedPath extends RsPathResolveKind {
        @Nonnull
        private final Set<Namespace> ns;

        public UnqualifiedPath(@Nonnull Set<Namespace> ns) {
            this.ns = ns;
        }

        @Nonnull
        public Set<Namespace> getNs() {
            return ns;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof UnqualifiedPath)) return false;
            return ns.equals(((UnqualifiedPath) o).ns);
        }

        @Override
        public int hashCode() {
            return ns.hashCode();
        }
    }

    /** {@code bar} in {@code foo::bar} or {@code use foo::\{bar\}} */
    public static final class QualifiedPath extends RsPathResolveKind {
        @Nonnull
        private final RsPath path;
        @Nonnull
        private final Set<Namespace> ns;
        @Nonnull
        private final RsPath qualifier;
        @Nullable
        private final PsiElement parent;

        public QualifiedPath(@Nonnull RsPath path, @Nonnull Set<Namespace> ns, @Nonnull RsPath qualifier, @Nullable PsiElement parent) {
            this.path = path;
            this.ns = ns;
            this.qualifier = qualifier;
            this.parent = parent;
        }

        @Nonnull
        public RsPath getPath() {
            return path;
        }

        @Nonnull
        public Set<Namespace> getNs() {
            return ns;
        }

        @Nonnull
        public RsPath getQualifier() {
            return qualifier;
        }

        @Nullable
        public PsiElement getParent() {
            return parent;
        }
    }

    /** {@code <Foo>::bar} or {@code <Foo as Bar>::baz} */
    public static final class ExplicitTypeQualifiedPath extends RsPathResolveKind {
        @Nonnull
        private final Set<Namespace> ns;
        @Nonnull
        private final RsTypeQual typeQual;

        public ExplicitTypeQualifiedPath(@Nonnull Set<Namespace> ns, @Nonnull RsTypeQual typeQual) {
            this.ns = ns;
            this.typeQual = typeQual;
        }

        @Nonnull
        public Set<Namespace> getNs() {
            return ns;
        }

        @Nonnull
        public RsTypeQual getTypeQual() {
            return typeQual;
        }
    }

    /** A $crate path from macro expansion */
    public static final class MacroDollarCrateIdentifier extends RsPathResolveKind {
        @Nonnull
        private final RsPath path;

        public MacroDollarCrateIdentifier(@Nonnull RsPath path) {
            this.path = path;
        }

        @Nonnull
        public RsPath getPath() {
            return path;
        }
    }

    /**
     * A crate-relative path (e.g. in visibility restrictions or 2015 edition paths starting with ::).
     */
    public static final class CrateRelativePath extends RsPathResolveKind {
        @Nonnull
        private final RsPath path;
        @Nonnull
        private final Set<Namespace> ns;
        private final boolean hasColonColon;

        public CrateRelativePath(@Nonnull RsPath path, @Nonnull Set<Namespace> ns, boolean hasColonColon) {
            this.path = path;
            this.ns = ns;
            this.hasColonColon = hasColonColon;
        }

        @Nonnull
        public RsPath getPath() {
            return path;
        }

        @Nonnull
        public Set<Namespace> getNs() {
            return ns;
        }

        public boolean getHasColonColon() {
            return hasColonColon;
        }
    }

    /** A path starting with :: since 2018 edition */
    public static final class ExternCratePath extends RsPathResolveKind {
        public static final ExternCratePath INSTANCE = new ExternCratePath();
        private ExternCratePath() {}
    }

    /** Item path in {@code dyn Iterator<Item = u8>} */
    public static final class AssocTypeBindingPath extends RsPathResolveKind {
        @Nonnull
        private final RsAssocTypeBinding parent;

        public AssocTypeBindingPath(@Nonnull RsAssocTypeBinding parent) {
            this.parent = parent;
        }

        @Nonnull
        public RsAssocTypeBinding getParentBinding() {
            return parent;
        }
    }
}
