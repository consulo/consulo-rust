/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
        @NotNull
        private final Set<Namespace> ns;

        public UnqualifiedPath(@NotNull Set<Namespace> ns) {
            this.ns = ns;
        }

        @NotNull
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
        @NotNull
        private final RsPath path;
        @NotNull
        private final Set<Namespace> ns;
        @NotNull
        private final RsPath qualifier;
        @Nullable
        private final PsiElement parent;

        public QualifiedPath(@NotNull RsPath path, @NotNull Set<Namespace> ns, @NotNull RsPath qualifier, @Nullable PsiElement parent) {
            this.path = path;
            this.ns = ns;
            this.qualifier = qualifier;
            this.parent = parent;
        }

        @NotNull
        public RsPath getPath() {
            return path;
        }

        @NotNull
        public Set<Namespace> getNs() {
            return ns;
        }

        @NotNull
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
        @NotNull
        private final Set<Namespace> ns;
        @NotNull
        private final RsTypeQual typeQual;

        public ExplicitTypeQualifiedPath(@NotNull Set<Namespace> ns, @NotNull RsTypeQual typeQual) {
            this.ns = ns;
            this.typeQual = typeQual;
        }

        @NotNull
        public Set<Namespace> getNs() {
            return ns;
        }

        @NotNull
        public RsTypeQual getTypeQual() {
            return typeQual;
        }
    }

    /** A $crate path from macro expansion */
    public static final class MacroDollarCrateIdentifier extends RsPathResolveKind {
        @NotNull
        private final RsPath path;

        public MacroDollarCrateIdentifier(@NotNull RsPath path) {
            this.path = path;
        }

        @NotNull
        public RsPath getPath() {
            return path;
        }
    }

    /**
     * A crate-relative path (e.g. in visibility restrictions or 2015 edition paths starting with ::).
     */
    public static final class CrateRelativePath extends RsPathResolveKind {
        @NotNull
        private final RsPath path;
        @NotNull
        private final Set<Namespace> ns;
        private final boolean hasColonColon;

        public CrateRelativePath(@NotNull RsPath path, @NotNull Set<Namespace> ns, boolean hasColonColon) {
            this.path = path;
            this.ns = ns;
            this.hasColonColon = hasColonColon;
        }

        @NotNull
        public RsPath getPath() {
            return path;
        }

        @NotNull
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
        @NotNull
        private final RsAssocTypeBinding parent;

        public AssocTypeBindingPath(@NotNull RsAssocTypeBinding parent) {
            this.parent = parent;
        }

        @NotNull
        public RsAssocTypeBinding getParentBinding() {
            return parent;
        }
    }
}
