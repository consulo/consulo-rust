/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.codeEditor.HighlighterIterator;
import consulo.language.BracePair;
import consulo.language.Language;
import consulo.language.PairedBraceMatcher;
import consulo.language.ast.IElementType;
import consulo.language.editor.highlight.LanguageBraceMatcher;
import consulo.language.editor.highlight.NontrivialBraceMatcher;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapts a {@link PairedBraceMatcher} (a simple {@link BracePair} table) to the richer
 * {@link consulo.language.editor.highlight.BraceMatcher} contract the editor consumes.
 * <p>
 * Consulo ships exactly this as {@code consulo.language.editor.internal.PairedBraceMatcherAdapter},
 * but that package is exported only to platform modules. Every interface involved
 * ({@link NontrivialBraceMatcher}, {@link LanguageBraceMatcher}, {@link PairedBraceMatcher},
 * {@link BracePair}) is public, so only the glue needed re-creating.
 */
public class RsPairedBraceMatcherAdapter implements NontrivialBraceMatcher, LanguageBraceMatcher {

    private final PairedBraceMatcher myMatcher;
    private final Language myLanguage;

    public RsPairedBraceMatcherAdapter(@Nonnull PairedBraceMatcher matcher, @Nonnull Language language) {
        myMatcher = matcher;
        myLanguage = language;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return myLanguage;
    }

    @Override
    public int getBraceTokenGroupId(IElementType tokenType) {
        for (BracePair pair : myMatcher.getPairs()) {
            if (tokenType == pair.getLeftBraceType() || tokenType == pair.getRightBraceType()) {
                return myLanguage.hashCode();
            }
        }
        return -1;
    }

    @Nullable
    public BracePair findPair(boolean left, HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
        IElementType tokenType = (IElementType) iterator.getTokenType();
        for (BracePair pair : myMatcher.getPairs()) {
            if (tokenType == (left ? pair.getLeftBraceType() : pair.getRightBraceType())) return pair;
        }
        return null;
    }

    @Override
    public boolean isLBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
        return findPair(true, iterator, fileText, fileType) != null;
    }

    @Override
    public boolean isRBraceToken(HighlighterIterator iterator, CharSequence fileText, FileType fileType) {
        return findPair(false, iterator, fileText, fileType) != null;
    }

    @Override
    public IElementType getOppositeBraceTokenType(IElementType type) {
        for (BracePair pair : myMatcher.getPairs()) {
            if (type == pair.getRightBraceType()) return pair.getLeftBraceType();
            if (type == pair.getLeftBraceType()) return pair.getRightBraceType();
        }
        return null;
    }

    @Override
    public boolean isPairBraces(IElementType tokenType, IElementType tokenType2) {
        for (BracePair pair : myMatcher.getPairs()) {
            if (tokenType == pair.getLeftBraceType() && tokenType2 == pair.getRightBraceType()
                || tokenType == pair.getRightBraceType() && tokenType2 == pair.getLeftBraceType()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isStructuralBrace(HighlighterIterator iterator, CharSequence text, FileType fileType) {
        IElementType tokenType = (IElementType) iterator.getTokenType();
        for (BracePair pair : myMatcher.getPairs()) {
            if (tokenType == pair.getRightBraceType() || tokenType == pair.getLeftBraceType()) {
                return pair.isStructural();
            }
        }
        return false;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(IElementType lbraceType, @Nullable IElementType contextType) {
        return myMatcher.isPairedBracesAllowedBeforeType(lbraceType, contextType);
    }

    @Override
    public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
        return myMatcher.getCodeConstructStart(file, openingBraceOffset);
    }

    @Nonnull
    @Override
    public List<IElementType> getOppositeBraceTokenTypes(IElementType type) {
        List<IElementType> result = null;
        for (BracePair pair : myMatcher.getPairs()) {
            IElementType opposite = null;
            if (type == pair.getRightBraceType()) opposite = pair.getLeftBraceType();
            else if (type == pair.getLeftBraceType()) opposite = pair.getRightBraceType();

            if (opposite != null) {
                if (result == null) result = new ArrayList<>(2);
                result.add(opposite);
            }
        }
        return result == null ? Collections.emptyList() : result;
    }
}
