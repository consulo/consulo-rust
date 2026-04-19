/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.MirSpan;
import org.rust.lang.core.types.regions.Scope;

import java.util.List;
import java.util.Objects;

public class ThirBlock {
    @NotNull
    public final Scope regionScope;
    @Nullable
    public final Scope destructionScope;
    @NotNull
    public final List<ThirStatement> statements;
    @Nullable
    public final ThirExpr expr;
    @NotNull
    public final MirSpan source;

    public ThirBlock(
        @NotNull Scope regionScope,
        @Nullable Scope destructionScope,
        @NotNull List<ThirStatement> statements,
        @Nullable ThirExpr expr,
        @NotNull MirSpan source
    ) {
        this.regionScope = regionScope;
        this.destructionScope = destructionScope;
        this.statements = statements;
        this.expr = expr;
        this.source = source;
    }

    @NotNull
    public Scope getRegionScope() {
        return regionScope;
    }

    @Nullable
    public Scope getDestructionScope() {
        return destructionScope;
    }

    @NotNull
    public List<ThirStatement> getStatements() {
        return statements;
    }

    @Nullable
    public ThirExpr getExpr() {
        return expr;
    }

    @NotNull
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
