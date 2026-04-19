/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.mir.schemas.MirMatch.MirMatchPair;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.ext.RsEnumItemImplMixin;
import org.rust.lang.core.psi.ext.RsEnumItemUtil;
import org.rust.lang.core.thir.ThirPat;

import java.util.BitSet;

public abstract class MirTest {
    @NotNull
    private final MirSpan span;

    protected MirTest(@NotNull MirSpan span) {
        this.span = span;
    }

    @NotNull
    public MirSpan getSpan() {
        return span;
    }

    public abstract int targets();

    /** Test what enum variant a value is. */
    public static final class Switch extends MirTest {
        /** The enum type being tested. */
        @NotNull
        private final RsEnumItem item;
        /** The set of variants that we should create a branch for. We also create an additional "otherwise" case. */
        @NotNull
        private final BitSet variants;

        public Switch(@NotNull MirSpan span, @NotNull RsEnumItem item, @NotNull BitSet variants) {
            super(span);
            this.item = item;
            this.variants = variants;
        }

        @NotNull
        public RsEnumItem getItem() {
            return item;
        }

        @NotNull
        public BitSet getVariants() {
            return variants;
        }

        @Override
        public int targets() {
            return RsEnumItemUtil.getVariants(item).size() + 1;
        }
    }

    public static final class SwitchInt extends MirTest {
        public SwitchInt(@NotNull MirSpan span) {
            super(span);
        }

        @Override
        public int targets() {
            throw new UnsupportedOperationException("TODO");
        }
    }

    public static final class Eq extends MirTest {
        public Eq(@NotNull MirSpan span) {
            super(span);
        }

        @Override
        public int targets() {
            return 2;
        }
    }

    public static final class Range extends MirTest {
        public Range(@NotNull MirSpan span) {
            super(span);
        }

        @Override
        public int targets() {
            return 2;
        }
    }

    public static final class Len extends MirTest {
        public Len(@NotNull MirSpan span) {
            super(span);
        }

        @Override
        public int targets() {
            return 2;
        }
    }

    @NotNull
    public static MirTest test(@NotNull MirMatchPair matchPair) {
        MirSpan span = matchPair.getPattern().getSource();
        ThirPat pattern = matchPair.getPattern();
        if (pattern instanceof ThirPat.Variant) {
            ThirPat.Variant variant = (ThirPat.Variant) pattern;
            return new Switch(
                span,
                variant.getItem(),
                new BitSet(RsEnumItemUtil.getVariants(variant.getItem()).size())
            );
        } else if (pattern instanceof ThirPat.Const) {
            throw new UnsupportedOperationException("TODO");
        } else if (pattern instanceof ThirPat.Range) {
            throw new UnsupportedOperationException("TODO");
        } else if (pattern instanceof ThirPat.Slice) {
            throw new UnsupportedOperationException("TODO");
        } else if (pattern instanceof ThirPat.Or) {
            throw new IllegalStateException("Or-patterns should have already been handled");
        } else if (pattern instanceof ThirPat.AscribeUserType
            || pattern instanceof ThirPat.Array
            || pattern instanceof ThirPat.Wild
            || pattern instanceof ThirPat.Binding
            || pattern instanceof ThirPat.Leaf
            || pattern instanceof ThirPat.Deref) {
            throw new IllegalStateException("Simplifiable pattern found");
        }
        throw new IllegalStateException("Unexpected pattern type: " + pattern);
    }
}
