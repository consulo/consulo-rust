/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RsAlignmentStrategy {

    /**
     * Requests current strategy for alignment to use for given child.
     */
    @Nullable
    Alignment getAlignment(@NotNull ASTNode child, @Nullable ASTNode parent, @NotNull RsFmtContext childCtx);

    /**
     * Always returns {@code null}.
     */
    RsAlignmentStrategy NullStrategy = new RsAlignmentStrategy() {
        @Nullable
        @Override
        public Alignment getAlignment(@NotNull ASTNode child, @Nullable ASTNode parent, @NotNull RsFmtContext childCtx) {
            return null;
        }
    };

    /**
     * Apply this strategy only when child element is in {@code tt}.
     */
    default RsAlignmentStrategy alignIf(@NotNull IElementType... tt) {
        return alignIf(TokenSet.create(tt));
    }

    /**
     * Apply this strategy only when child element type matches {@code filterSet}.
     */
    default RsAlignmentStrategy alignIf(@NotNull TokenSet filterSet) {
        RsAlignmentStrategy outer = this;
        return new RsAlignmentStrategy() {
            @Nullable
            @Override
            public Alignment getAlignment(@NotNull ASTNode child, @Nullable ASTNode parent, @NotNull RsFmtContext childCtx) {
                if (filterSet.contains(child.getElementType())) {
                    return outer.getAlignment(child, parent, childCtx);
                } else {
                    return null;
                }
            }
        };
    }

    /**
     * Apply this strategy only when {@code predicate} passes.
     */
    default RsAlignmentStrategy alignIf(@NotNull AlignmentPredicate predicate) {
        RsAlignmentStrategy outer = this;
        return new RsAlignmentStrategy() {
            @Nullable
            @Override
            public Alignment getAlignment(@NotNull ASTNode child, @Nullable ASTNode parent, @NotNull RsFmtContext childCtx) {
                if (predicate.test(child, parent, childCtx)) {
                    return outer.getAlignment(child, parent, childCtx);
                } else {
                    return null;
                }
            }
        };
    }

    /**
     * Returns {@link #NullStrategy} if {@code condition} is {@code false}. Useful for making strategies configurable.
     */
    default RsAlignmentStrategy alignIf(boolean condition) {
        if (condition) {
            return this;
        } else {
            return NullStrategy;
        }
    }

    /**
     * Always returns {@code alignment}.
     */
    static RsAlignmentStrategy wrap(@NotNull Alignment alignment) {
        return new RsAlignmentStrategy() {
            @NotNull
            @Override
            public Alignment getAlignment(@NotNull ASTNode child, @Nullable ASTNode parent, @NotNull RsFmtContext childCtx) {
                return alignment;
            }
        };
    }

    /**
     * Always returns {@code alignment} (with default alignment).
     */
    static RsAlignmentStrategy wrap() {
        return wrap(Alignment.createAlignment());
    }

    /**
     * Always returns {@link RsFmtContext#getSharedAlignment()}
     */
    static RsAlignmentStrategy shared() {
        return new RsAlignmentStrategy() {
            @Nullable
            @Override
            public Alignment getAlignment(@NotNull ASTNode child, @Nullable ASTNode parent, @NotNull RsFmtContext childCtx) {
                return childCtx.getSharedAlignment();
            }
        };
    }

    @FunctionalInterface
    interface AlignmentPredicate {
        boolean test(@NotNull ASTNode child, @Nullable ASTNode parent, @NotNull RsFmtContext ctx);
    }
}
