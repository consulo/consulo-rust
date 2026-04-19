/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull
    private final String openText;
    @NotNull
    private final String closeText;
    @NotNull
    private final IElementType openToken;
    @NotNull
    private final IElementType closeToken;

    MacroBraces(@NotNull String openText, @NotNull String closeText,
                @NotNull IElementType openToken, @NotNull IElementType closeToken) {
        this.openText = openText;
        this.closeText = closeText;
        this.openToken = openToken;
        this.closeToken = closeToken;
    }

    @NotNull
    public String getOpenText() { return openText; }

    @NotNull
    public String getCloseText() { return closeText; }

    @NotNull
    public IElementType getOpenToken() { return openToken; }

    @NotNull
    public IElementType getCloseToken() { return closeToken; }

    @NotNull
    public String wrap(@NotNull CharSequence text) {
        return openText + text + closeText;
    }

    public boolean getNeedsSemicolon() {
        return this != BRACES;
    }

    @Nullable
    public static MacroBraces fromToken(@NotNull IElementType token) {
        if (token == RsElementTypes.LPAREN || token == RsElementTypes.RPAREN) return PARENS;
        if (token == RsElementTypes.LBRACK || token == RsElementTypes.RBRACK) return BRACKS;
        if (token == RsElementTypes.LBRACE || token == RsElementTypes.RBRACE) return BRACES;
        return null;
    }

    @Nullable
    public static MacroBraces fromOpenToken(@NotNull IElementType token) {
        if (token == RsElementTypes.LPAREN) return PARENS;
        if (token == RsElementTypes.LBRACK) return BRACKS;
        if (token == RsElementTypes.LBRACE) return BRACES;
        return null;
    }

    @NotNull
    public static MacroBraces fromTokenOrFail(@NotNull IElementType token) {
        MacroBraces result = fromToken(token);
        if (result == null) {
            throw new IllegalStateException("Given token is not a brace: " + token);
        }
        return result;
    }
}
