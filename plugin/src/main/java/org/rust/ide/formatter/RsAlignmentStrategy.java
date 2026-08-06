/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import consulo.language.codeStyle.Alignment;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface RsAlignmentStrategy {

    /**
     * Requests current strategy for alignment to use for given child.
     */
    @Nullable
    Alignment getAlignment(@Nonnull ASTNode child, @Nullable ASTNode parent, @Nonnull RsFmtContext childCtx);

    /**
     * Always returns {@code null}.
     */
    RsAlignmentStrategy NullStrategy = new RsAlignmentStrategy() {
        @Nullable
        @Override
        public Alignment getAlignment(@Nonnull ASTNode child, @Nullable ASTNode parent, @Nonnull RsFmtContext childCtx) {
            return null;
        }
    };

    /**
     * Apply this strategy only when child element is in {@code tt}.
     */
    default RsAlignmentStrategy alignIf(@Nonnull IElementType... tt) {
        return alignIf(TokenSet.create(tt));
    }

    /**
     * Apply this strategy only when child element type matches {@code filterSet}.
     */
    default RsAlignmentStrategy alignIf(@Nonnull TokenSet filterSet) {
        RsAlignmentStrategy outer = this;
        return new RsAlignmentStrategy() {
            @Nullable
            @Override
            public Alignment getAlignment(@Nonnull ASTNode child, @Nullable ASTNode parent, @Nonnull RsFmtContext childCtx) {
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
    default RsAlignmentStrategy alignIf(@Nonnull AlignmentPredicate predicate) {
        RsAlignmentStrategy outer = this;
        return new RsAlignmentStrategy() {
            @Nullable
            @Override
            public Alignment getAlignment(@Nonnull ASTNode child, @Nullable ASTNode parent, @Nonnull RsFmtContext childCtx) {
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
    static RsAlignmentStrategy wrap(@Nonnull Alignment alignment) {
        return new RsAlignmentStrategy() {
            @Nonnull
            @Override
            public Alignment getAlignment(@Nonnull ASTNode child, @Nullable ASTNode parent, @Nonnull RsFmtContext childCtx) {
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
            public Alignment getAlignment(@Nonnull ASTNode child, @Nullable ASTNode parent, @Nonnull RsFmtContext childCtx) {
                return childCtx.getSharedAlignment();
            }
        };
    }

    @FunctionalInterface
    interface AlignmentPredicate {
        boolean test(@Nonnull ASTNode child, @Nullable ASTNode parent, @Nonnull RsFmtContext ctx);
    }
}
