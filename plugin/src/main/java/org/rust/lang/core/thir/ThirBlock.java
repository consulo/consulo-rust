/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.schemas.MirSpan;
import org.rust.lang.core.types.regions.Scope;

import java.util.List;
import java.util.Objects;

public class ThirBlock {
    @Nonnull
    public final Scope regionScope;
    @Nullable
    public final Scope destructionScope;
    @Nonnull
    public final List<ThirStatement> statements;
    @Nullable
    public final ThirExpr expr;
    @Nonnull
    public final MirSpan source;

    public ThirBlock(
        @Nonnull Scope regionScope,
        @Nullable Scope destructionScope,
        @Nonnull List<ThirStatement> statements,
        @Nullable ThirExpr expr,
        @Nonnull MirSpan source
    ) {
        this.regionScope = regionScope;
        this.destructionScope = destructionScope;
        this.statements = statements;
        this.expr = expr;
        this.source = source;
    }

    @Nonnull
    public Scope getRegionScope() {
        return regionScope;
    }

    @Nullable
    public Scope getDestructionScope() {
        return destructionScope;
    }

    @Nonnull
    public List<ThirStatement> getStatements() {
        return statements;
    }

    @Nullable
    public ThirExpr getExpr() {
        return expr;
    }

    @Nonnull
    public MirSpan getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ThirBlock)) return false;
        ThirBlock that = (ThirBlock) o;
        return regionScope.equals(that.regionScope) &&
            Objects.equals(destructionScope, that.destructionScope) &&
            statements.equals(that.statements) &&
            Objects.equals(expr, that.expr) &&
            source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(regionScope, destructionScope, statements, expr, source);
    }
}
