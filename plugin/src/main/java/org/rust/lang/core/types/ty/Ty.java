/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.Kind;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.infer.TypeFoldable;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

/**
 * Represents both a type, like `i32` or `S<Foo, Bar>`, as well
 * as an unbound constructor `S`.
 *
 * The name `Ty` is short for `Type`, inspired by the Rust
 * compiler.
 */
public abstract class Ty implements Kind, TypeFoldable<Ty> {
    private final int myFlags;

    protected Ty() {
        this(0);
    }

    protected Ty(int flags) {
        myFlags = flags;
    }

    @Override
    public int getFlags() {
        return myFlags;
    }

    @Override
    @Nonnull
    public Ty foldWith(@Nonnull TypeFolder folder) {
        return folder.foldTy(this);
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        return this;
    }

    @Override
    public boolean visitWith(@Nonnull TypeVisitor visitor) {
        return visitor.visitTy(this);
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        return false;
    }

    @Nullable
    public BoundElement<RsTypeAlias> getAliasedBy() {
        return null;
    }

    @Nonnull
    public Ty withAlias(@Nonnull BoundElement<RsTypeAlias> aliasedBy) {
        return this;
    }

    /**
     * Bindings between formal type parameters and actual type arguments.
     */
    @Nonnull
    public Substitution getTypeParameterValues() {
        return SubstitutionUtil.EMPTY_SUBSTITUTION;
    }

    @Override
    @Nonnull
    public final String toString() {
        return TypeRendering.render(this);
    }

    /**
     * Use it instead of equals if you want to check that the types are the same from the Rust perspective.
     */
    public boolean isEquivalentTo(@Nullable Ty other) {
        return other != null && isEquivalentToInner(other);
    }

    protected boolean isEquivalentToInner(@Nonnull Ty other) {
        return equals(other);
    }
}
