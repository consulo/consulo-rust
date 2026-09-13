package org.rust.lang.core.parser;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.BracePair;
import consulo.language.Language;
import consulo.language.PairedBraceMatcher;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.ast.TokenType;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.impl.RsTokenSets;

/**
 * Brace pairs for Rust.
 * <p>
 * Consulo exposes brace matching through two separate extension points: this one, which the parser reads
 * via {@link PairedBraceMatcher#forLanguage} to drive brace-balanced error recovery, and
 * {@code consulo.language.editor.highlight.LanguageBraceMatcher}, which the editor reads for highlighting
 * and auto-insertion. Both have to be registered; the editor side lives in the IDE layer.
 */
@ExtensionImpl
public class RsPairedBraceMatcher implements PairedBraceMatcher {
    private static final BracePair[] PAIRS = new BracePair[]{
        new BracePair(RsElementTypes.LBRACE, RsElementTypes.RBRACE, true /* structural */),
        new BracePair(RsElementTypes.LPAREN, RsElementTypes.RPAREN, false),
        new BracePair(RsElementTypes.LBRACK, RsElementTypes.RBRACK, false),
        new BracePair(RsElementTypes.LT, RsElementTypes.GT, false)
    };

    private static final TokenSet INSERT_PAIR_BRACE_BEFORE = TokenSet.orSet(
        RsTokenSets.RS_COMMENTS,
        TokenSet.create(
            TokenType.WHITE_SPACE,
            RsElementTypes.SEMICOLON,
            RsElementTypes.COMMA,
            RsElementTypes.RPAREN,
            RsElementTypes.RBRACK,
            RsElementTypes.RBRACE,
            RsElementTypes.LBRACE
        )
    );

    @Nonnull
    @Override
    public Language getLanguage() {
        return RsLanguage.INSTANCE;
    }

    @Override
    public BracePair[] getPairs() {
        return PAIRS;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(IElementType lbraceType, IElementType next) {
        return next == null || INSERT_PAIR_BRACE_BEFORE.contains(next);
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return openingBraceOffset;
    }
}
