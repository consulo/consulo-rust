/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.proc.ProMacroExpanderVersion;
import org.rust.lang.core.psi.MacroBraces;

import java.util.*;

/**
 * A flat representation of a token tree, used for efficient serialization/deserialization
 * when communicating with the proc macro expander process.
 *
 * @see <a href="https://github.com/rust-analyzer/rust-analyzer/blob/3e4ac8a2c9136052/crates/proc_macro_api/src/msg/flat.rs">rust-analyzer flat.rs</a>
 */
public class FlatTree {
    @NotNull
    private final IntArrayList mySubtree;
    @NotNull
    private final IntArrayList myLiteral;
    @NotNull
    private final IntArrayList myPunct;
    @NotNull
    private final IntArrayList myIdent;
    @NotNull
    private final IntArrayList myTokenTree;
    @NotNull
    private final List<String> myText;

    public FlatTree(
        @NotNull IntArrayList subtree,
        @NotNull IntArrayList literal,
        @NotNull IntArrayList punct,
        @NotNull IntArrayList ident,
        @NotNull IntArrayList tokenTree,
        @NotNull List<String> text
    ) {
        mySubtree = subtree;
        myLiteral = literal;
        myPunct = punct;
        myIdent = ident;
        myTokenTree = tokenTree;
        myText = text;
    }

    @NotNull
    public IntArrayList getSubtree() {
        return mySubtree;
    }

    @NotNull
    public IntArrayList getLiteral() {
        return myLiteral;
    }

    @NotNull
    public IntArrayList getPunct() {
        return myPunct;
    }

    @NotNull
    public IntArrayList getIdent() {
        return myIdent;
    }

    @NotNull
    public IntArrayList getTokenTree() {
        return myTokenTree;
    }

    @NotNull
    public List<String> getText() {
        return myText;
    }

    @NotNull
    public TokenTree.Subtree toTokenTree(@NotNull ProMacroExpanderVersion version) {
        boolean encodeCloseSpan = version.compareTo(ProMacroExpanderVersion.ENCODE_CLOSE_SPAN_VERSION) >= 0;
        int offset = encodeCloseSpan ? 1 : 0;
        int stepSize = encodeCloseSpan ? 5 : 4;

        int numSubtrees = mySubtree.size() / stepSize;
        @SuppressWarnings("unchecked")
        TokenTree.Subtree[] res = new TokenTree.Subtree[numSubtrees];

        for (int i = mySubtree.size() - stepSize; i >= 0; i -= stepSize) {
            int delimiterId = mySubtree.getInt(i);
            int kind = mySubtree.getInt(i + offset + 1);
            int lo = mySubtree.getInt(i + offset + 2);
            int len = mySubtree.getInt(i + offset + 3);

            List<TokenTree> tokenTrees = new ArrayList<>(len - lo);
            for (int j = lo; j < len; j++) {
                int idxTag = myTokenTree.getInt(j);
                int tag = idxTag & 0b11;
                int idx = idxTag >> 2;
                switch (tag) {
                    case 0b00:
                        tokenTrees.add(res[idx]);
                        break;
                    case 0b01: {
                        int index = idx * 2;
                        int tokenId = myLiteral.getInt(index);
                        int textIdx = myLiteral.getInt(index + 1);
                        tokenTrees.add(new TokenTree.Leaf.Literal(myText.get(textIdx), tokenId));
                        break;
                    }
                    case 0b10: {
                        int index = idx * 3;
                        int tokenId = myPunct.getInt(index);
                        char chr = (char) myPunct.getInt(index + 1);
                        Spacing spacing;
                        switch (myPunct.getInt(index + 2)) {
                            case 0:
                                spacing = Spacing.Alone;
                                break;
                            case 1:
                                spacing = Spacing.Joint;
                                break;
                            default:
                                throw new IllegalStateException("Unknown spacing " + myPunct.getInt(index + 2));
                        }
                        tokenTrees.add(new TokenTree.Leaf.Punct(String.valueOf(chr), spacing, tokenId));
                        break;
                    }
                    case 0b11: {
                        int index = idx * 2;
                        int tokenId = myIdent.getInt(index);
                        int textIdx = myIdent.getInt(index + 1);
                        tokenTrees.add(new TokenTree.Leaf.Ident(myText.get(textIdx), tokenId));
                        break;
                    }
                    default:
                        throw new IllegalStateException("Bad tag " + tag);
                }
            }

            MacroBraces delimiterKind;
            switch (kind) {
                case 0:
                    delimiterKind = null;
                    break;
                case 1:
                    delimiterKind = MacroBraces.PARENS;
                    break;
                case 2:
                    delimiterKind = MacroBraces.BRACES;
                    break;
                case 3:
                    delimiterKind = MacroBraces.BRACKS;
                    break;
                default:
                    throw new IllegalStateException("Unknown kind " + kind);
            }

            Delimiter delimiter = delimiterKind != null ? new Delimiter(delimiterId, delimiterKind) : null;
            res[i / stepSize] = new TokenTree.Subtree(delimiter, tokenTrees);
        }

        return res[0];
    }

    @NotNull
    public static FlatTree fromSubtree(@NotNull TokenTree.Subtree root, @NotNull ProMacroExpanderVersion version) {
        FlatTreeBuilder builder = new FlatTreeBuilder(version.compareTo(ProMacroExpanderVersion.ENCODE_CLOSE_SPAN_VERSION) >= 0);
        builder.write(root);
        return builder.toFlatTree();
    }

    // --- FlatTreeBuilder (private inner helper) ---

    private static class FlatTreeBuilder {
        private final boolean myEncodeCloseSpan;
        @NotNull
        private final Deque<int[]> myWork; // int[0] = idx, stores subtree pairs
        @NotNull
        private final Map<String, Integer> myStringTable;

        @NotNull
        private final IntArrayList subtree;
        @NotNull
        private final IntArrayList literal;
        @NotNull
        private final IntArrayList punct;
        @NotNull
        private final IntArrayList ident;
        @NotNull
        private final IntArrayList tokenTree;
        @NotNull
        private final List<String> text;
        @NotNull
        private final List<TokenTree.Subtree> workSubtrees;

        FlatTreeBuilder(boolean encodeCloseSpan) {
            myEncodeCloseSpan = encodeCloseSpan;
            myWork = new ArrayDeque<>();
            myStringTable = new HashMap<>();
            subtree = new IntArrayList();
            literal = new IntArrayList();
            punct = new IntArrayList();
            ident = new IntArrayList();
            tokenTree = new IntArrayList();
            text = new ArrayList<>();
            workSubtrees = new ArrayList<>();
        }

        @NotNull
        FlatTree toFlatTree() {
            return new FlatTree(subtree, literal, punct, ident, tokenTree, text);
        }

        void write(@NotNull TokenTree.Subtree root) {
            enqueue(root);
            while (!myWork.isEmpty()) {
                int[] entry = myWork.pollFirst();
                int idx = entry[0];
                TokenTree.Subtree sub = workSubtrees.get(idx);
                processSubtree(idx, sub);
            }
        }

        private void processSubtree(int subtreeId, @NotNull TokenTree.Subtree sub) {
            int firstTt = tokenTree.size();
            int nTt = sub.getTokenTrees().size();
            tokenTree.ensureCapacity(firstTt + nTt);
            for (int i = tokenTree.size(); i < firstTt + nTt; i++) {
                tokenTree.add(-1);
            }

            int offset = myEncodeCloseSpan ? 1 : 0;
            int stepSize = myEncodeCloseSpan ? 5 : 4;

            subtree.set(subtreeId * stepSize + offset + 2, firstTt);
            subtree.set(subtreeId * stepSize + offset + 3, firstTt + nTt);

            for (TokenTree child : sub.getTokenTrees()) {
                int idxTag;
                if (child instanceof TokenTree.Subtree) {
                    int idx = enqueue((TokenTree.Subtree) child);
                    idxTag = (idx << 2) | 0b00;
                } else if (child instanceof TokenTree.Leaf.Literal) {
                    TokenTree.Leaf.Literal lit = (TokenTree.Leaf.Literal) child;
                    int idx = literal.size() / 2;
                    int textIdx = intern(lit.getText());
                    literal.add(lit.getId());
                    literal.add(textIdx);
                    idxTag = (idx << 2) | 0b01;
                } else if (child instanceof TokenTree.Leaf.Punct) {
                    TokenTree.Leaf.Punct p = (TokenTree.Leaf.Punct) child;
                    int idx = punct.size() / 3;
                    punct.add(p.getId());
                    punct.add(p.getChar().charAt(0));
                    punct.add(p.getSpacing() == Spacing.Alone ? 0 : 1);
                    idxTag = (idx << 2) | 0b10;
                } else if (child instanceof TokenTree.Leaf.Ident) {
                    TokenTree.Leaf.Ident id = (TokenTree.Leaf.Ident) child;
                    int idx = ident.size() / 2;
                    int textIdx = intern(id.getText());
                    ident.add(id.getId());
                    ident.add(textIdx);
                    idxTag = (idx << 2) | 0b11;
                } else {
                    throw new IllegalStateException("Unknown TokenTree type: " + child.getClass());
                }
                tokenTree.set(firstTt, idxTag);
                firstTt++;
            }
        }

        private int enqueue(@NotNull TokenTree.Subtree sub) {
            int stepSize = myEncodeCloseSpan ? 5 : 4;
            int idx = subtree.size() / stepSize;
            int delimiterId = sub.getDelimiter() != null ? sub.getDelimiter().getId() : -1;
            MacroBraces delimiterKind = sub.getDelimiter() != null ? sub.getDelimiter().getKind() : null;

            subtree.add(delimiterId);
            if (myEncodeCloseSpan) {
                subtree.add(-1); // closeId
            }
            int kindValue;
            if (delimiterKind == null) {
                kindValue = 0;
            } else if (delimiterKind == MacroBraces.PARENS) {
                kindValue = 1;
            } else if (delimiterKind == MacroBraces.BRACES) {
                kindValue = 2;
            } else { // BRACKS
                kindValue = 3;
            }
            subtree.add(kindValue);
            subtree.add(-1);
            subtree.add(-1);

            // Ensure workSubtrees list is big enough
            while (workSubtrees.size() <= idx) {
                workSubtrees.add(null);
            }
            workSubtrees.set(idx, sub);
            myWork.addLast(new int[]{idx});

            return idx;
        }

        private int intern(@NotNull String t) {
            Integer existing = myStringTable.get(t);
            if (existing != null) return existing;
            int idx = text.size();
            text.add(t);
            myStringTable.put(t, idx);
            return idx;
        }
    }
}
