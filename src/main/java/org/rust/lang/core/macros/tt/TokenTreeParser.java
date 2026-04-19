/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.parser.RustParserUtil;
import org.rust.lang.core.psi.*;

import java.util.ArrayList;
import java.util.List;

import static org.rust.lang.core.psi.RsElementTypes.*;

/**
 * Parses a PsiBuilder token stream into a {@link TokenTree}.
 */
public final class TokenTreeParser {

    private TokenTreeParser() {}

    @NotNull
    public static MappedSubtree parseSubtree(@NotNull PsiBuilder builder) {
        return parseSubtree(builder, 0, 0);
    }

    @NotNull
    public static MappedSubtree parseSubtree(@NotNull PsiBuilder builder, int textOffset, int idOffset) {
        TokenTreeParserImpl parser = new TokenTreeParserImpl(builder, textOffset, idOffset);
        return parser.parse();
    }

    // --- Private implementation ---

    private static final TokenSet PROC_MACRO_IDENTIFIER_TOKENS = TokenSet.orSet(
        RsTokenType.RS_IDENTIFIER_TOKENS,
        RsTokenType.tokenSetOf(UNDERSCORE)
    );

    private static final TokenSet NEXT_TOKEN_ALONE_SET = TokenSet.orSet(
        RsTokenType.tokenSetOf(TokenType.WHITE_SPACE, LBRACK, LBRACE, LPAREN, QUOTE_IDENTIFIER),
        RsTokenType.RS_COMMENTS,
        RsTokenType.RS_LITERALS,
        PROC_MACRO_IDENTIFIER_TOKENS
    );

    private static final TokenSet WHITESPACE_OR_COMMENTS = TokenSet.orSet(
        RsTokenType.tokenSetOf(TokenType.WHITE_SPACE),
        RsTokenType.RS_COMMENTS
    );

    private static class TokenTreeParserImpl {
        @NotNull
        private final PsiBuilder myLexer;
        private final int myTextOffset;
        private final int myIdOffset;
        @NotNull
        private final List<TokenMetadata> myTokenMap;

        TokenTreeParserImpl(@NotNull PsiBuilder lexer, int textOffset, int idOffset) {
            myLexer = lexer;
            myTextOffset = textOffset;
            myIdOffset = idOffset;
            myTokenMap = new ArrayList<>();
        }

        @NotNull
        MappedSubtree parse() {
            List<TokenTree> result = new ArrayList<>();

            while (true) {
                IElementType tokenType = myLexer.getTokenType();
                if (tokenType == null) break;
                int offset = myLexer.getCurrentOffset();
                parseToken(offset, tokenType, result);
            }

            if (result.size() == 1 && result.get(0) instanceof TokenTree.Subtree) {
                return new MappedSubtree((TokenTree.Subtree) result.get(0), new TokenMap(myTokenMap));
            }

            return new MappedSubtree(new TokenTree.Subtree(null, result), new TokenMap(myTokenMap));
        }

        private void parseToken(int offset, @NotNull IElementType tokenType, @NotNull List<TokenTree> result) {
            MacroBraces delimKind = MacroBraces.fromOpenToken(tokenType);
            if (delimKind != null) {
                parseSubtreeDelim(offset, delimKind, result);
            } else {
                parseLeaf(offset, tokenType, result);
            }
        }

        private void parseSubtreeDelim(int offset, @NotNull MacroBraces delimKind, @NotNull List<TokenTree> result) {
            Delimiter delimLeaf = new Delimiter(allocDelimId(offset, nextWhitespaceOrCommentText(true), delimKind), delimKind);
            List<TokenTree> subtreeResult = new ArrayList<>();

            myLexer.advanceLexer();

            while (true) {
                IElementType tokenType = myLexer.getTokenType();

                if (tokenType == null) {
                    result.add(punct(delimKind.getOpenText(), Spacing.Alone, offset, ""));
                    result.addAll(subtreeResult);
                    return;
                }

                if (tokenType == delimKind.getCloseToken()) break;

                parseToken(myLexer.getCurrentOffset(), tokenType, subtreeResult);
            }

            closeDelim(delimLeaf.getId(), myLexer.getCurrentOffset(), nextWhitespaceOrCommentText(true));

            result.add(new TokenTree.Subtree(delimLeaf, subtreeResult));
            myLexer.advanceLexer();
        }

        private void parseLeaf(int offset, @NotNull IElementType tokenType, @NotNull List<TokenTree> result) {
            boolean shouldAdvanceLexer = true;
            String tokenText = myLexer.getTokenText();
            if (tokenText == null) tokenText = "";

            if (tokenType == INTEGER_LITERAL) {
                var lastMarker = myLexer.getLatestDoneMarker();
                if (RustParserUtil.parseFloatLiteral(myLexer, 0)) {
                    shouldAdvanceLexer = false;
                    var floatMarker = myLexer.getLatestDoneMarker();
                    String tokenText2;
                    if (floatMarker != null && floatMarker != lastMarker) {
                        tokenText2 = myLexer.getOriginalText()
                            .subSequence(floatMarker.getStartOffset(), floatMarker.getEndOffset()).toString();
                    } else {
                        tokenText2 = tokenText;
                    }
                    result.add(lit(tokenText2, offset, nextWhitespaceOrCommentText(shouldAdvanceLexer)));
                } else {
                    result.add(lit(tokenText, offset, nextWhitespaceOrCommentText(true)));
                }
            } else if (RsTokenType.RS_LITERALS.contains(tokenType)) {
                result.add(lit(tokenText, offset));
            } else if (PROC_MACRO_IDENTIFIER_TOKENS.contains(tokenType)) {
                result.add(ident(tokenText, offset));
            } else if (tokenType == QUOTE_IDENTIFIER) {
                result.add(punct(String.valueOf(tokenText.charAt(0)), Spacing.Joint, offset, ""));
                result.add(ident(tokenText.substring(1), offset + 1));
            } else {
                for (int i = 0; i < tokenText.length(); i++) {
                    boolean isLastChar = i == tokenText.length() - 1;
                    String ch = String.valueOf(tokenText.charAt(i));
                    Spacing spacing;
                    CharSequence rightTrivia;
                    if (!isLastChar) {
                        spacing = Spacing.Joint;
                        rightTrivia = "";
                    } else {
                        IElementType nextRawToken = myLexer.rawLookup(1);
                        if (nextRawToken == null) {
                            spacing = Spacing.Alone;
                            rightTrivia = "";
                        } else if (NEXT_TOKEN_ALONE_SET.contains(nextRawToken)) {
                            spacing = Spacing.Alone;
                            rightTrivia = nextWhitespaceOrCommentText(true);
                        } else {
                            spacing = Spacing.Joint;
                            rightTrivia = "";
                        }
                    }
                    result.add(punct(ch, spacing, offset + i, rightTrivia));
                }
            }

            if (shouldAdvanceLexer) {
                myLexer.advanceLexer();
            }
        }

        @NotNull
        private TokenTree.Leaf.Ident ident(@NotNull String text, int startOffset) {
            return ident(text, startOffset, nextWhitespaceOrCommentText(true));
        }

        @NotNull
        private TokenTree.Leaf.Ident ident(@NotNull String text, int startOffset, @NotNull CharSequence rightTrivia) {
            TokenTree.Leaf.Ident leaf = new TokenTree.Leaf.Ident(text, nextId());
            writeMeta(startOffset, rightTrivia, leaf);
            return leaf;
        }

        @NotNull
        private TokenTree.Leaf.Literal lit(@NotNull String text, int startOffset) {
            return lit(text, startOffset, nextWhitespaceOrCommentText(true));
        }

        @NotNull
        private TokenTree.Leaf.Literal lit(@NotNull String text, int startOffset, @NotNull CharSequence rightTrivia) {
            TokenTree.Leaf.Literal leaf = new TokenTree.Leaf.Literal(text, nextId());
            writeMeta(startOffset, rightTrivia, leaf);
            return leaf;
        }

        @NotNull
        private TokenTree.Leaf.Punct punct(@NotNull String text, @NotNull Spacing spacing, int startOffset, @NotNull CharSequence rightTrivia) {
            TokenTree.Leaf.Punct leaf = new TokenTree.Leaf.Punct(text, spacing, nextId());
            writeMeta(startOffset, rightTrivia, leaf);
            return leaf;
        }

        private int nextId() {
            return myIdOffset + myTokenMap.size();
        }

        private void writeMeta(int startOffset, @NotNull CharSequence rightTrivia, @NotNull TokenTree.Leaf leaf) {
            assert nextId() == leaf.getId();
            myTokenMap.add(new TokenMetadata.Token(myTextOffset + startOffset, rightTrivia, leaf));
        }

        private int allocDelimId(int openOffset, @NotNull CharSequence rightTrivia, @NotNull MacroBraces delimKind) {
            int id = nextId();
            TokenMetadata.Delimiter.DelimiterPart open =
                new TokenMetadata.Delimiter.DelimiterPart(myTextOffset + openOffset, rightTrivia);
            myTokenMap.add(new TokenMetadata.Delimiter(open, null, delimKind));
            return id;
        }

        private void closeDelim(int tokenId, int closeOffset, @NotNull CharSequence rightTrivia) {
            int index = tokenId - myIdOffset;
            TokenMetadata.Delimiter existing = (TokenMetadata.Delimiter) myTokenMap.get(index);
            myTokenMap.set(index, existing.copy(
                new TokenMetadata.Delimiter.DelimiterPart(myTextOffset + closeOffset, rightTrivia)
            ));
        }

        @NotNull
        private CharSequence nextWhitespaceOrCommentText(boolean startFromNextToken) {
            int start = startFromNextToken ? 1 : 0;
            int counter = start;
            while (true) {
                IElementType t = myLexer.rawLookup(counter);
                if (t == null || !WHITESPACE_OR_COMMENTS.contains(t)) break;
                counter++;
            }
            if (counter == start) return "";
            int startOffset = myLexer.rawTokenTypeStart(start);
            int endOffset = myLexer.rawTokenTypeStart(counter);
            return myLexer.getOriginalText().subSequence(startOffset, endOffset);
        }
    }
}
