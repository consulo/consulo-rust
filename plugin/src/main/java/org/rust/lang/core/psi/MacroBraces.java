/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public enum MacroBraces {
    @SerializedName("Parenthesis")
    @JsonProperty("Parenthesis")
    PARENS("(", ")", RsElementTypes.LPAREN, RsElementTypes.RPAREN),

    @SerializedName("Bracket")
    @JsonProperty("Bracket")
    BRACKS("[", "]", RsElementTypes.LBRACK, RsElementTypes.RBRACK),

    @SerializedName("Brace")
    @JsonProperty("Brace")
    BRACES("{", "}", RsElementTypes.LBRACE, RsElementTypes.RBRACE);

    @Nonnull
    private final String openText;
    @Nonnull
    private final String closeText;
    @Nonnull
    private final IElementType openToken;
    @Nonnull
    private final IElementType closeToken;

    MacroBraces(@Nonnull String openText, @Nonnull String closeText,
                @Nonnull IElementType openToken, @Nonnull IElementType closeToken) {
        this.openText = openText;
        this.closeText = closeText;
        this.openToken = openToken;
        this.closeToken = closeToken;
    }

    @Nonnull
    public String getOpenText() { return openText; }

    @Nonnull
    public String getCloseText() { return closeText; }

    @Nonnull
    public IElementType getOpenToken() { return openToken; }

    @Nonnull
    public IElementType getCloseToken() { return closeToken; }

    @Nonnull
    public String wrap(@Nonnull CharSequence text) {
        return openText + text + closeText;
    }

    public boolean getNeedsSemicolon() {
        return this != BRACES;
    }

    @Nullable
    public static MacroBraces fromToken(@Nonnull IElementType token) {
        if (token == RsElementTypes.LPAREN || token == RsElementTypes.RPAREN) return PARENS;
        if (token == RsElementTypes.LBRACK || token == RsElementTypes.RBRACK) return BRACKS;
        if (token == RsElementTypes.LBRACE || token == RsElementTypes.RBRACE) return BRACES;
        return null;
    }

    @Nullable
    public static MacroBraces fromOpenToken(@Nonnull IElementType token) {
        if (token == RsElementTypes.LPAREN) return PARENS;
        if (token == RsElementTypes.LBRACK) return BRACKS;
        if (token == RsElementTypes.LBRACE) return BRACES;
        return null;
    }

    @Nonnull
    public static MacroBraces fromTokenOrFail(@Nonnull IElementType token) {
        MacroBraces result = fromToken(token);
        if (result == null) {
            throw new IllegalStateException("Given token is not a brace: " + token);
        }
        return result;
    }
}
