/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class FormatParameter {
    @Nonnull
    private final ParameterMatchInfo myMatchInfo;
    @Nonnull
    private final ParameterLookup myLookup;

    protected FormatParameter(@Nonnull ParameterMatchInfo matchInfo, @Nonnull ParameterLookup lookup) {
        this.myMatchInfo = matchInfo;
        this.myLookup = lookup;
    }

    @Nonnull
    public ParameterMatchInfo getMatchInfo() {
        return myMatchInfo;
    }

    @Nonnull
    public ParameterLookup getLookup() {
        return myLookup;
    }

    @Nonnull
    public TextRange getRange() {
        return myMatchInfo.getRange();
    }

    @Override
    public String toString() {
        return String.valueOf(myMatchInfo.getText());
    }

    public static class Value extends FormatParameter {
        @Nonnull
        private final String myTypeStr;
        @Nonnull
        private final TextRange myTypeRange;
        @Nullable
        private final FormatTraitType myType;

        public Value(@Nonnull ParameterMatchInfo matchInfo, @Nonnull ParameterLookup lookup, @Nonnull String typeStr, @Nonnull TextRange typeRange) {
            super(matchInfo, lookup);
            this.myTypeStr = typeStr;
            this.myTypeRange = typeRange;
            this.myType = FormatTraitType.forString(typeStr);
        }

        @Nonnull
        public String getTypeStr() {
            return myTypeStr;
        }

        @Nonnull
        public TextRange getTypeRange() {
            return myTypeRange;
        }

        @Nullable
        public FormatTraitType getType() {
            return myType;
        }
    }

    public static class Specifier extends FormatParameter {
        @Nonnull
        private final String mySpecifier;

        public Specifier(@Nonnull ParameterMatchInfo matchInfo, @Nonnull ParameterLookup lookup, @Nonnull String specifier) {
            super(matchInfo, lookup);
            this.mySpecifier = specifier;
        }

        @Nonnull
        public String getSpecifier() {
            return mySpecifier;
        }
    }
}
