/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * {@link TokenTree} is a kind of AST used to communicate with Rust procedural macros.
 * <p>
 * A procedural macro (defined in terms of {@link TokenTree}s) is a function that accepts a {@link TokenTree} and
 * returns a {@link TokenTree}.
 */
public abstract class TokenTree {

    private TokenTree() {}

    public abstract static class Leaf extends TokenTree {
        public abstract int getId();

        public static final class Literal extends Leaf {
            @NotNull
            private final String myText;
            private final int myId;

            public Literal(@NotNull String text, int id) {
                myText = text;
                myId = id;
            }

            @NotNull
            public String getText() {
                return myText;
            }

            @Override
            public int getId() {
                return myId;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Literal)) return false;
                Literal lit = (Literal) o;
                return myId == lit.myId && Objects.equals(myText, lit.myText);
            }

            @Override
            public int hashCode() {
                return Objects.hash(myText, myId);
            }

            @Override
            public String toString() {
                return "Literal(text=" + myText + ", id=" + myId + ")";
            }
        }

        public static final class Punct extends Leaf {
            @NotNull
            private final String myChar;
            @NotNull
            private final Spacing mySpacing;
            private final int myId;

            public Punct(@NotNull String aChar, @NotNull Spacing spacing, int id) {
                myChar = aChar;
                mySpacing = spacing;
                myId = id;
            }

            @NotNull
            public String getChar() {
                return myChar;
            }

            @NotNull
            public Spacing getSpacing() {
                return mySpacing;
            }

            @Override
            public int getId() {
                return myId;
            }

            @NotNull
            public Punct copy(int newId) {
                return new Punct(myChar, mySpacing, newId);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Punct)) return false;
                Punct p = (Punct) o;
                return myId == p.myId && Objects.equals(myChar, p.myChar) && mySpacing == p.mySpacing;
            }

            @Override
            public int hashCode() {
                return Objects.hash(myChar, mySpacing, myId);
            }

            @Override
            public String toString() {
                return "Punct(char=" + myChar + ", spacing=" + mySpacing + ", id=" + myId + ")";
            }
        }

        public static final class Ident extends Leaf {
            @NotNull
            private final String myText;
            private final int myId;

            public Ident(@NotNull String text, int id) {
                myText = text;
                myId = id;
            }

            @NotNull
            public String getText() {
                return myText;
            }

            @Override
            public int getId() {
                return myId;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Ident)) return false;
                Ident ident = (Ident) o;
                return myId == ident.myId && Objects.equals(myText, ident.myText);
            }

            @Override
            public int hashCode() {
                return Objects.hash(myText, myId);
            }

            @Override
            public String toString() {
                return "Ident(text=" + myText + ", id=" + myId + ")";
            }
        }
    }

    public static final class Subtree extends TokenTree {
        @Nullable
        private final Delimiter myDelimiter;
        @NotNull
        private final List<TokenTree> myTokenTrees;

        public Subtree(@Nullable Delimiter delimiter, @NotNull List<TokenTree> tokenTrees) {
            myDelimiter = delimiter;
            myTokenTrees = tokenTrees;
        }

        @Nullable
        public Delimiter getDelimiter() {
            return myDelimiter;
        }

        @NotNull
        public List<TokenTree> getTokenTrees() {
            return myTokenTrees;
        }

        @NotNull
        public Subtree copy(@Nullable Delimiter newDelimiter) {
            return new Subtree(newDelimiter, myTokenTrees);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Subtree)) return false;
            Subtree s = (Subtree) o;
            return Objects.equals(myDelimiter, s.myDelimiter)
                && Objects.equals(myTokenTrees, s.myTokenTrees);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myDelimiter, myTokenTrees);
        }

        @Override
        public String toString() {
            return "Subtree(delimiter=" + myDelimiter + ", tokenTrees=" + myTokenTrees.size() + ")";
        }
    }

    // --- Debug printing ---

    @NotNull
    public static String toDebugString(@NotNull TokenTree tree) {
        StringBuilder sb = new StringBuilder();
        debugPrintTokenTree(sb, tree, 0);
        return sb.toString();
    }

    private static void debugPrintTokenTree(@NotNull StringBuilder sb, @NotNull TokenTree tree, int level) {
        sb.append("  ".repeat(level));
        if (tree instanceof Leaf) {
            debugPrintLeaf(sb, (Leaf) tree);
        } else if (tree instanceof Subtree) {
            debugPrintSubtree(sb, (Subtree) tree, level);
        }
    }

    private static void debugPrintSubtree(@NotNull StringBuilder sb, @NotNull Subtree subtree, int level) {
        String aux;
        if (subtree.getDelimiter() == null) {
            aux = "$";
        } else {
            aux = subtree.getDelimiter().getKind().getOpenText()
                + subtree.getDelimiter().getKind().getCloseText()
                + " " + subtree.getDelimiter().getId();
        }
        sb.append("SUBTREE ").append(aux);
        for (TokenTree tokenTree : subtree.getTokenTrees()) {
            sb.append("\n");
            debugPrintTokenTree(sb, tokenTree, level + 1);
        }
    }

    private static void debugPrintLeaf(@NotNull StringBuilder sb, @NotNull Leaf leaf) {
        if (leaf instanceof Leaf.Literal) {
            Leaf.Literal lit = (Leaf.Literal) leaf;
            sb.append("LITERAL ").append(lit.getText()).append(" ").append(lit.getId());
        } else if (leaf instanceof Leaf.Punct) {
            Leaf.Punct punct = (Leaf.Punct) leaf;
            sb.append("PUNCT   ").append(punct.getChar()).append(" [")
                .append(punct.getSpacing().toString().toLowerCase()).append("] ").append(punct.getId());
        } else if (leaf instanceof Leaf.Ident) {
            Leaf.Ident ident = (Leaf.Ident) leaf;
            sb.append("IDENT   ").append(ident.getText()).append(" ").append(ident.getId());
        }
    }
}
