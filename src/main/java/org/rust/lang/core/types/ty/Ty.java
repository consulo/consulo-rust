/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    @NotNull
    public Ty foldWith(@NotNull TypeFolder folder) {
        return folder.foldTy(this);
    }

    @Override
    @NotNull
    public Ty superFoldWith(@NotNull TypeFolder folder) {
        return this;
    }

    @Override
    public boolean visitWith(@NotNull TypeVisitor visitor) {
        return visitor.visitTy(this);
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        return false;
    }

    @Nullable
    public BoundElement<RsTypeAlias> getAliasedBy() {
        return null;
    }

    @NotNull
    public Ty withAlias(@NotNull BoundElement<RsTypeAlias> aliasedBy) {
        return this;
    }

    /**
     * Bindings between formal type parameters and actual type arguments.
     */
    @NotNull
    public Substitution getTypeParameterValues() {
        return SubstitutionUtil.EMPTY_SUBSTITUTION;
    }

    @Override
    @NotNull
    public final String toString() {
        return TypeRendering.render(this);
    }

    /**
     * Use it instead of equals if you want to check that the types are the same from the Rust perspective.
     */
    public boolean isEquivalentTo(@Nullable Ty other) {
        return other != null && isEquivalentToInner(other);
    }

    protected boolean isEquivalentToInner(@NotNull Ty other) {
        return equals(other);
    }
}
