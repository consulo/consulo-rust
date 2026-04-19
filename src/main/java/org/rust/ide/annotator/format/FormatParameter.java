/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class FormatParameter {
    @NotNull
    private final ParameterMatchInfo myMatchInfo;
    @NotNull
    private final ParameterLookup myLookup;

    protected FormatParameter(@NotNull ParameterMatchInfo matchInfo, @NotNull ParameterLookup lookup) {
        this.myMatchInfo = matchInfo;
        this.myLookup = lookup;
    }

    @NotNull
    public ParameterMatchInfo getMatchInfo() {
        return myMatchInfo;
    }

    @NotNull
    public ParameterLookup getLookup() {
        return myLookup;
    }

    @NotNull
    public TextRange getRange() {
        return myMatchInfo.getRange();
    }

    @Override
    public String toString() {
        return myMatchInfo.getText();
    }

    public static class Value extends FormatParameter {
        @NotNull
        private final String myTypeStr;
        @NotNull
        private final TextRange myTypeRange;
        @Nullable
        private final FormatTraitType myType;

        public Value(@NotNull ParameterMatchInfo matchInfo, @NotNull ParameterLookup lookup, @NotNull String typeStr, @NotNull TextRange typeRange) {
            super(matchInfo, lookup);
            this.myTypeStr = typeStr;
            this.myTypeRange = typeRange;
            this.myType = FormatTraitType.forString(typeStr);
        }

        @NotNull
        public String getTypeStr() {
            return myTypeStr;
        }

        @NotNull
        public TextRange getTypeRange() {
            return myTypeRange;
        }

        @Nullable
        public FormatTraitType getType() {
            return myType;
        }
    }

    public static class Specifier extends FormatParameter {
        @NotNull
        private final String mySpecifier;

        public Specifier(@NotNull ParameterMatchInfo matchInfo, @NotNull ParameterLookup lookup, @NotNull String specifier) {
            super(matchInfo, lookup);
            this.mySpecifier = specifier;
        }

        @NotNull
        public String getSpecifier() {
            return mySpecifier;
        }
    }
}
