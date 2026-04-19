/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.MacroBraces;

import java.util.Objects;

/**
 * Metadata associated with a token, used for mapping between source and expanded text.
 */
public abstract class TokenMetadata {

    private TokenMetadata() {}

    public static final class Token extends TokenMetadata {
        private final int myStartOffset;
        @NotNull
        private final CharSequence myRightTrivia;
        @NotNull
        private final TokenTree.Leaf myOrigin;

        public Token(int startOffset, @NotNull CharSequence rightTrivia, @NotNull TokenTree.Leaf origin) {
            myStartOffset = startOffset;
            myRightTrivia = rightTrivia;
            myOrigin = origin;
        }

        public int getStartOffset() {
            return myStartOffset;
        }

        @NotNull
        public CharSequence getRightTrivia() {
            return myRightTrivia;
        }

        @NotNull
        public TokenTree.Leaf getOrigin() {
            return myOrigin;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Token)) return false;
            Token t = (Token) o;
            return myStartOffset == t.myStartOffset
                && Objects.equals(myRightTrivia.toString(), t.myRightTrivia.toString())
                && Objects.equals(myOrigin, t.myOrigin);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myStartOffset, myRightTrivia.toString(), myOrigin);
        }
    }

    public static final class Delimiter extends TokenMetadata {
        @NotNull
        private final DelimiterPart myOpen;
        @Nullable
        private final DelimiterPart myClose;
        @NotNull
        private final MacroBraces myOriginKind;

        public Delimiter(@NotNull DelimiterPart open, @Nullable DelimiterPart close, @NotNull MacroBraces originKind) {
            myOpen = open;
            myClose = close;
            myOriginKind = originKind;
        }

        @NotNull
        public DelimiterPart getOpen() {
            return myOpen;
        }

        @Nullable
        public DelimiterPart getClose() {
            return myClose;
        }

        @NotNull
        public MacroBraces getOriginKind() {
            return myOriginKind;
        }

        @NotNull
        public Delimiter copy(@Nullable DelimiterPart newClose) {
            return new Delimiter(myOpen, newClose, myOriginKind);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Delimiter)) return false;
            Delimiter d = (Delimiter) o;
            return Objects.equals(myOpen, d.myOpen)
                && Objects.equals(myClose, d.myClose)
                && myOriginKind == d.myOriginKind;
        }

        @Override
        public int hashCode() {
            return Objects.hash(myOpen, myClose, myOriginKind);
        }

        public static final class DelimiterPart {
            private final int myStartOffset;
            @NotNull
            private final CharSequence myRightTrivia;

            public DelimiterPart(int startOffset, @NotNull CharSequence rightTrivia) {
                myStartOffset = startOffset;
                myRightTrivia = rightTrivia;
            }

            public int getStartOffset() {
                return myStartOffset;
            }

            @NotNull
            public CharSequence getRightTrivia() {
                return myRightTrivia;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof DelimiterPart)) return false;
                DelimiterPart d = (DelimiterPart) o;
                return myStartOffset == d.myStartOffset
                    && Objects.equals(myRightTrivia.toString(), d.myRightTrivia.toString());
            }

            @Override
            public int hashCode() {
                return Objects.hash(myStartOffset, myRightTrivia.toString());
            }
        }
    }
}
