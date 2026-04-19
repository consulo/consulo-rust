/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt;

import com.intellij.util.SmartList;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.MappedTextRange;
import org.rust.lang.core.macros.RangeMap;
import org.rust.lang.core.psi.MacroBraces;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A token tree subtree paired with its token map.
 */
public final class MappedSubtree {
    @NotNull
    private final TokenTree.Subtree mySubtree;
    @NotNull
    private final TokenMap myTokenMap;

    public MappedSubtree(@NotNull TokenTree.Subtree subtree, @NotNull TokenMap tokenMap) {
        mySubtree = subtree;
        myTokenMap = tokenMap;
    }

    @NotNull
    public TokenTree.Subtree getSubtree() {
        return mySubtree;
    }

    @NotNull
    public TokenMap getTokenMap() {
        return myTokenMap;
    }

    /**
     * Converts this mapped subtree to text with a range map.
     */
    @NotNull
    public Pair<CharSequence, RangeMap> toMappedText() {
        TokenTree.Subtree recoveredSubtree = new SubtreeIdRecovery(mySubtree, myTokenMap).recover();
        return new SubtreeTextBuilder(recoveredSubtree, myTokenMap).toText();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MappedSubtree)) return false;
        MappedSubtree that = (MappedSubtree) o;
        return Objects.equals(mySubtree, that.mySubtree) && Objects.equals(myTokenMap, that.myTokenMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mySubtree, myTokenMap);
    }

    // --- SubtreeTextBuilder (private inner helper) ---

    private static class SubtreeTextBuilder {
        @NotNull
        private final TokenTree.Subtree mySubtree;
        @NotNull
        private final TokenMap myTokenMap;
        @NotNull
        private final StringBuilder mySb;
        @NotNull
        private final List<MappedTextRange> myRanges;

        SubtreeTextBuilder(@NotNull TokenTree.Subtree subtree, @NotNull TokenMap tokenMap) {
            mySubtree = subtree;
            myTokenMap = tokenMap;
            mySb = new StringBuilder();
            myRanges = new SmartList<>();
        }

        @NotNull
        Pair<CharSequence, RangeMap> toText() {
            appendSubtree(mySubtree, true);
            return new Pair<>(mySb, new RangeMap(myRanges));
        }

        private void appendSubtree(@NotNull TokenTree.Subtree subtree, boolean isRootSubtree) {
            if (subtree.getDelimiter() != null) {
                appendDelimiterPart(subtree.getDelimiter(), true);
            }
            List<TokenTree> tokenTrees = subtree.getTokenTrees();
            for (int i = 0; i < tokenTrees.size(); i++) {
                TokenTree tokenTree = tokenTrees.get(i);
                TokenTree nextTokenTree = i + 1 < tokenTrees.size() ? tokenTrees.get(i + 1) : null;
                if (tokenTree instanceof TokenTree.Leaf) {
                    appendLeaf((TokenTree.Leaf) tokenTree, nextTokenTree, isRootSubtree || subtree.getDelimiter() != null);
                } else if (tokenTree instanceof TokenTree.Subtree) {
                    appendSubtree((TokenTree.Subtree) tokenTree, false);
                }
            }
            if (subtree.getDelimiter() != null) {
                appendDelimiterPart(subtree.getDelimiter(), false);
            }
        }

        private void appendLeaf(@NotNull TokenTree.Leaf leaf, @Nullable TokenTree nextTokenTree, boolean hasDelimiter) {
            String text;
            Spacing spacing;
            if (leaf instanceof TokenTree.Leaf.Literal) {
                text = ((TokenTree.Leaf.Literal) leaf).getText();
                spacing = Spacing.Alone;
            } else if (leaf instanceof TokenTree.Leaf.Ident) {
                text = ((TokenTree.Leaf.Ident) leaf).getText();
                spacing = Spacing.Alone;
            } else if (leaf instanceof TokenTree.Leaf.Punct) {
                text = ((TokenTree.Leaf.Punct) leaf).getChar();
                spacing = ((TokenTree.Leaf.Punct) leaf).getSpacing();
            } else {
                return;
            }

            TokenMetadata rawMeta = myTokenMap.get(leaf.getId());
            TokenMetadata.Token meta = null;
            if (rawMeta instanceof TokenMetadata.Token) {
                TokenMetadata.Token tokenMeta = (TokenMetadata.Token) rawMeta;
                if (tokenMeta.getOrigin().equals(leaf)) {
                    meta = tokenMeta;
                }
            }

            if (meta != null) {
                RangeMap.mergeAdd(myRanges,
                    new MappedTextRange(meta.getStartOffset(), mySb.length(), text.length() + meta.getRightTrivia().length()));
            }
            mySb.append(text);
            if (meta != null) {
                mySb.append(meta.getRightTrivia());

                boolean canOmitSpace;
                if (nextTokenTree instanceof TokenTree.Leaf) {
                    // id >= 0 && id + 1 == nextTokenTree.id
                    canOmitSpace = leaf.getId() >= 0 && leaf.getId() + 1 == ((TokenTree.Leaf) nextTokenTree).getId();
                } else if (nextTokenTree instanceof TokenTree.Subtree) {
                    canOmitSpace = true;
                } else {
                    // null means end of parent subtree
                    canOmitSpace = hasDelimiter;
                }

                if (meta.getRightTrivia().length() == 0 && spacing == Spacing.Alone && !canOmitSpace) {
                    mySb.append(" ");
                }
            } else if (spacing == Spacing.Alone) {
                mySb.append(" ");
            }
        }

        private void appendDelimiterPart(@NotNull Delimiter delimiter, boolean open) {
            TokenMetadata rawMeta = myTokenMap.get(delimiter.getId());
            TokenMetadata.Delimiter.DelimiterPart meta = null;
            if (rawMeta instanceof TokenMetadata.Delimiter) {
                TokenMetadata.Delimiter delimMeta = (TokenMetadata.Delimiter) rawMeta;
                meta = open ? delimMeta.getOpen() : delimMeta.getClose();
            }
            if (meta != null) {
                RangeMap.mergeAdd(myRanges,
                    new MappedTextRange(meta.getStartOffset(), mySb.length(), 1 + meta.getRightTrivia().length()));
            }
            String braceText = open ? delimiter.getKind().getOpenText() : delimiter.getKind().getCloseText();
            mySb.append(braceText);
            if (meta != null) {
                mySb.append(meta.getRightTrivia());
            }
        }
    }

    // --- SubtreeIdRecovery (private inner helper) ---

    /**
     * Real procedural macros tend to discard spans (token ids) from {@link TokenTree.Leaf.Punct} tokens
     * and subtree {@link Delimiter}s. This routine tries to recover them using a simple heuristic:
     * if a token without a mapping (with id == -1) follows a token <i>with</i> a mapping, then it most likely
     * should be mapped with the next token in the source.
     */
    private static class SubtreeIdRecovery {
        @NotNull
        private final TokenTree.Subtree mySubtree;
        @NotNull
        private final TokenMap myTokenMap;
        private int myPreviousLeafId;

        SubtreeIdRecovery(@NotNull TokenTree.Subtree subtree, @NotNull TokenMap tokenMap) {
            mySubtree = subtree;
            myTokenMap = tokenMap;
            myPreviousLeafId = -1;
        }

        @NotNull
        TokenTree.Subtree recover() {
            return processSubtree(mySubtree);
        }

        @NotNull
        private TokenTree.Subtree processSubtree(@NotNull TokenTree.Subtree subtree) {
            Delimiter adjustedDelimiter = subtree.getDelimiter() != null
                ? processDelimiter(subtree.getDelimiter())
                : null;
            if (adjustedDelimiter != null) {
                myPreviousLeafId = adjustedDelimiter.getId();
            }
            List<TokenTree> adjustedTokenTrees = new ArrayList<>(subtree.getTokenTrees().size());
            for (TokenTree tokenTree : subtree.getTokenTrees()) {
                if (tokenTree instanceof TokenTree.Leaf) {
                    TokenTree.Leaf newLeaf;
                    if (tokenTree instanceof TokenTree.Leaf.Punct) {
                        newLeaf = processLeaf((TokenTree.Leaf.Punct) tokenTree);
                    } else {
                        newLeaf = (TokenTree.Leaf) tokenTree;
                    }
                    myPreviousLeafId = newLeaf.getId();
                    adjustedTokenTrees.add(newLeaf);
                } else if (tokenTree instanceof TokenTree.Subtree) {
                    adjustedTokenTrees.add(processSubtree((TokenTree.Subtree) tokenTree));
                }
            }
            return new TokenTree.Subtree(adjustedDelimiter, adjustedTokenTrees);
        }

        @NotNull
        private TokenTree.Leaf processLeaf(@NotNull TokenTree.Leaf.Punct leaf) {
            if (leaf.getId() != -1 || myPreviousLeafId == -1) {
                return leaf;
            }
            int recoveredId = myPreviousLeafId + 1;
            TokenMetadata recoveredMeta = myTokenMap.get(recoveredId);
            if (!(recoveredMeta instanceof TokenMetadata.Token)) return leaf;
            TokenTree.Leaf.Punct newLeaf = leaf.copy(recoveredId);
            if (!newLeaf.equals(((TokenMetadata.Token) recoveredMeta).getOrigin())) {
                return leaf;
            }
            return newLeaf;
        }

        @NotNull
        private Delimiter processDelimiter(@NotNull Delimiter delimiter) {
            if (delimiter.getId() != -1 || myPreviousLeafId == -1) {
                return delimiter;
            }
            int recoveredId = myPreviousLeafId + 1;
            TokenMetadata recoveredMeta = myTokenMap.get(recoveredId);
            if (!(recoveredMeta instanceof TokenMetadata.Delimiter)) return delimiter;
            if (delimiter.getKind() != ((TokenMetadata.Delimiter) recoveredMeta).getOriginKind()) {
                return delimiter;
            }
            return delimiter.copy(recoveredId);
        }
    }
}
