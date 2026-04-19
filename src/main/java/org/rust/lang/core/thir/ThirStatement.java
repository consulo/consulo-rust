/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.types.regions.Scope;

import java.util.Objects;

public abstract class ThirStatement {
    @Nullable
    public abstract Scope getDestructionScope();

    private ThirStatement() {
    }

    public static class Let extends ThirStatement {
        @NotNull
        public final Scope remainderScope;
        @NotNull
        public final Scope initScope;
        @Nullable
        private final Scope destructionScope;
        @NotNull
        public final ThirPat pattern;
        @Nullable
        public final ThirExpr initializer;
        @Nullable
        public final ThirBlock elseBlock;

        public Let(
            @NotNull Scope remainderScope,
            @NotNull Scope initScope,
            @Nullable Scope destructionScope,
            @NotNull ThirPat pattern,
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

        @NotNull
        public Scope getRemainderScope() {
            return remainderScope;
        }

        @NotNull
        public Scope getInitScope() {
            return initScope;
        }

        @Nullable
        @Override
        public Scope getDestructionScope() {
            return destructionScope;
        }

        @NotNull
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
        @NotNull
        public final Scope scope;
        @Nullable
        private final Scope destructionScope;
        @NotNull
        public final ThirExpr expr;

        public Expr(
            @NotNull Scope scope,
            @Nullable Scope destructionScope,
            @NotNull ThirExpr expr
        ) {
            this.scope = scope;
            this.destructionScope = destructionScope;
            this.expr = expr;
        }

        @NotNull
        public Scope getScope() {
            return scope;
        }

        @Nullable
        @Override
        public Scope getDestructionScope() {
            return destructionScope;
        }

        @NotNull
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
