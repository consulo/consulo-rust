/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.errors;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.decl.FragmentKind;

import java.util.Objects;

public abstract class MacroMatchingError {
    private final int myOffsetInCallBody;

    MacroMatchingError(int offsetInCallBody) {
        myOffsetInCallBody = offsetInCallBody;
    }

    public int getOffsetInCallBody() {
        return myOffsetInCallBody;
    }

    @Override
    public String toString() {
        return "MacroMatchingError." + getClass().getSimpleName();
    }

    public static final class PatternSyntax extends MacroMatchingError {
        public PatternSyntax(int offsetInCallBody) {
            super(offsetInCallBody);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PatternSyntax that = (PatternSyntax) o;
            return getOffsetInCallBody() == that.getOffsetInCallBody();
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(getOffsetInCallBody());
        }
    }

    public static final class ExtraInput extends MacroMatchingError {
        public ExtraInput(int offsetInCallBody) {
            super(offsetInCallBody);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ExtraInput that = (ExtraInput) o;
            return getOffsetInCallBody() == that.getOffsetInCallBody();
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(getOffsetInCallBody());
        }
    }

    public static final class EndOfInput extends MacroMatchingError {
        public EndOfInput(int offsetInCallBody) {
            super(offsetInCallBody);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EndOfInput that = (EndOfInput) o;
            return getOffsetInCallBody() == that.getOffsetInCallBody();
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(getOffsetInCallBody());
        }
    }

    public static final class UnmatchedToken extends MacroMatchingError {
        private final String myExpectedTokenType;
        private final String myExpectedTokenText;
        private final String myActualTokenType;
        private final String myActualTokenText;

        public UnmatchedToken(
            int offsetInCallBody,
            @NotNull String expectedTokenType,
            @NotNull String expectedTokenText,
            @NotNull String actualTokenType,
            @NotNull String actualTokenText
        ) {
            super(offsetInCallBody);
            myExpectedTokenType = expectedTokenType;
            myExpectedTokenText = expectedTokenText;
            myActualTokenType = actualTokenType;
            myActualTokenText = actualTokenText;
        }

        @NotNull
        public String getExpectedTokenType() {
            return myExpectedTokenType;
        }

        @NotNull
        public String getExpectedTokenText() {
            return myExpectedTokenText;
        }

        @NotNull
        public String getActualTokenType() {
            return myActualTokenType;
        }

        @NotNull
        public String getActualTokenText() {
            return myActualTokenText;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UnmatchedToken that = (UnmatchedToken) o;
            return getOffsetInCallBody() == that.getOffsetInCallBody()
                && myExpectedTokenType.equals(that.myExpectedTokenType)
                && myExpectedTokenText.equals(that.myExpectedTokenText)
                && myActualTokenType.equals(that.myActualTokenType)
                && myActualTokenText.equals(that.myActualTokenText);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(getOffsetInCallBody());
            result = 31 * result + myExpectedTokenType.hashCode();
            result = 31 * result + myExpectedTokenText.hashCode();
            result = 31 * result + myActualTokenType.hashCode();
            result = 31 * result + myActualTokenText.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MacroMatchingError.UnmatchedToken(" +
                myExpectedTokenType + "(`" + myExpectedTokenText + "`) != " +
                myActualTokenType + "(`" + myActualTokenText + "`)" +
                ")";
        }
    }

    public static final class FragmentIsNotParsed extends MacroMatchingError {
        private final String myVariableName;
        private final FragmentKind myKind;

        public FragmentIsNotParsed(int offsetInCallBody, @NotNull String variableName, @NotNull FragmentKind kind) {
            super(offsetInCallBody);
            myVariableName = variableName;
            myKind = kind;
        }

        @NotNull
        public String getVariableName() {
            return myVariableName;
        }

        @NotNull
        public FragmentKind getKind() {
            return myKind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FragmentIsNotParsed that = (FragmentIsNotParsed) o;
            return getOffsetInCallBody() == that.getOffsetInCallBody()
                && myVariableName.equals(that.myVariableName)
                && myKind == that.myKind;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(getOffsetInCallBody());
            result = 31 * result + myVariableName.hashCode();
            result = 31 * result + myKind.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MacroMatchingError.FragmentIsNotParsed(" + myVariableName + ", " + myKind + ")";
        }
    }

    public static final class EmptyGroup extends MacroMatchingError {
        public EmptyGroup(int offsetInCallBody) {
            super(offsetInCallBody);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EmptyGroup that = (EmptyGroup) o;
            return getOffsetInCallBody() == that.getOffsetInCallBody();
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(getOffsetInCallBody());
        }
    }

    public static final class TooFewGroupElements extends MacroMatchingError {
        public TooFewGroupElements(int offsetInCallBody) {
            super(offsetInCallBody);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TooFewGroupElements that = (TooFewGroupElements) o;
            return getOffsetInCallBody() == that.getOffsetInCallBody();
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(getOffsetInCallBody());
        }
    }

    public static final class Nesting extends MacroMatchingError {
        private final String myVariableName;

        public Nesting(int offsetInCallBody, @NotNull String variableName) {
            super(offsetInCallBody);
            myVariableName = variableName;
        }

        @NotNull
        public String getVariableName() {
            return myVariableName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Nesting nesting = (Nesting) o;
            return getOffsetInCallBody() == nesting.getOffsetInCallBody()
                && myVariableName.equals(nesting.myVariableName);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(getOffsetInCallBody());
            result = 31 * result + myVariableName.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "MacroMatchingError.Nesting(" + myVariableName + ")";
        }
    }
}
