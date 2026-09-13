/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.types.regions.Scope;

import java.util.Objects;

public abstract class ThirStatement {
    @Nullable
    public abstract Scope getDestructionScope();

    private ThirStatement() {
    }

    public static class Let extends ThirStatement {
        @Nonnull
        public final Scope remainderScope;
        @Nonnull
        public final Scope initScope;
        @Nullable
        private final Scope destructionScope;
        @Nonnull
        public final ThirPat pattern;
        @Nullable
        public final ThirExpr initializer;
        @Nullable
        public final ThirBlock elseBlock;

        public Let(
            @Nonnull Scope remainderScope,
            @Nonnull Scope initScope,
            @Nullable Scope destructionScope,
            @Nonnull ThirPat pattern,
            @Nullable ThirExpr initializer,
            @Nullable ThirBlock elseBlock
        ) {
            this.remainderScope = remainderScope;
            this.initScope = initScope;
            this.destructionScope = destructionScope;
            this.pattern = pattern;
            this.initializer = initializer;
            this.elseBlock = elseBlock;
        }

        @Nonnull
        public Scope getRemainderScope() {
            return remainderScope;
        }

        @Nonnull
        public Scope getInitScope() {
            return initScope;
        }

        @Nullable
        @Override
        public Scope getDestructionScope() {
            return destructionScope;
        }

        @Nonnull
        public ThirPat getPattern() {
            return pattern;
        }

        @Nullable
        public ThirExpr getInitializer() {
            return initializer;
        }

        @Nullable
        public ThirBlock getElseBlock() {
            return elseBlock;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Let)) return false;
            Let that = (Let) o;
            return remainderScope.equals(that.remainderScope) &&
                initScope.equals(that.initScope) &&
                Objects.equals(destructionScope, that.destructionScope) &&
                pattern.equals(that.pattern) &&
                Objects.equals(initializer, that.initializer) &&
                Objects.equals(elseBlock, that.elseBlock);
        }

        @Override
        public int hashCode() {
            return Objects.hash(remainderScope, initScope, destructionScope, pattern, initializer, elseBlock);
        }
    }

    public static class Expr extends ThirStatement {
        @Nonnull
        public final Scope scope;
        @Nullable
        private final Scope destructionScope;
        @Nonnull
        public final ThirExpr expr;

        public Expr(
            @Nonnull Scope scope,
            @Nullable Scope destructionScope,
            @Nonnull ThirExpr expr
        ) {
            this.scope = scope;
            this.destructionScope = destructionScope;
            this.expr = expr;
        }

        @Nonnull
        public Scope getScope() {
            return scope;
        }

        @Nullable
        @Override
        public Scope getDestructionScope() {
            return destructionScope;
        }

        @Nonnull
        public ThirExpr getExpr() {
            return expr;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Expr)) return false;
            Expr that = (Expr) o;
            return scope.equals(that.scope) &&
                Objects.equals(destructionScope, that.destructionScope) &&
                expr.equals(that.expr);
        }

        @Override
        public int hashCode() {
            return Objects.hash(scope, destructionScope, expr);
        }
    }
}
