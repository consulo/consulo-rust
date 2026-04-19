/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMacroBinding;
import org.rust.lang.core.psi.RsMacroBindingGroup;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a parsed macro pattern structure used for building the macro graph.
 */
public abstract class Matcher {

    private Matcher() {}

    public static final class Literal extends Matcher {
        @NotNull
        private final ASTNode myValue;

        public Literal(@NotNull ASTNode value) {
            myValue = value;
        }

        @NotNull
        public ASTNode getValue() {
            return myValue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Literal)) return false;
            return Objects.equals(myValue, ((Literal) o).myValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myValue);
        }
    }

    public static final class Fragment extends Matcher {
        @NotNull
        private final FragmentKind myKind;

        public Fragment(@NotNull FragmentKind kind) {
            myKind = kind;
        }

        @NotNull
        public FragmentKind getKind() {
            return myKind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Fragment)) return false;
            return myKind == ((Fragment) o).myKind;
        }

        @Override
        public int hashCode() {
            return Objects.hash(myKind);
        }
    }

    public static final class Optional extends Matcher {
        @NotNull
        private final Matcher myMatcher;

        public Optional(@NotNull Matcher matcher) {
            myMatcher = matcher;
        }

        @NotNull
        public Matcher getMatcher() {
            return myMatcher;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Optional)) return false;
            return Objects.equals(myMatcher, ((Optional) o).myMatcher);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myMatcher);
        }
    }

    public static final class Choice extends Matcher {
        @NotNull
        private final List<Matcher> myMatchers;

        public Choice(@NotNull List<Matcher> matchers) {
            myMatchers = matchers;
        }

        @NotNull
        public List<Matcher> getMatchers() {
            return myMatchers;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Choice)) return false;
            return Objects.equals(myMatchers, ((Choice) o).myMatchers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myMatchers);
        }
    }

    public static final class Sequence extends Matcher {
        @NotNull
        private final List<Matcher> myMatchers;

        public Sequence(@NotNull List<Matcher> matchers) {
            myMatchers = matchers;
        }

        @NotNull
        public List<Matcher> getMatchers() {
            return myMatchers;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Sequence)) return false;
            return Objects.equals(myMatchers, ((Sequence) o).myMatchers);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myMatchers);
        }
    }

    public static final class Repeat extends Matcher {
        @NotNull
        private final List<Matcher> myMatchers;
        @Nullable
        private final ASTNode mySeparator;

        public Repeat(@NotNull List<Matcher> matchers, @Nullable ASTNode separator) {
            myMatchers = matchers;
            mySeparator = separator;
        }

        @NotNull
        public List<Matcher> getMatchers() {
            return myMatchers;
        }

        @Nullable
        public ASTNode getSeparator() {
            return mySeparator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Repeat)) return false;
            Repeat r = (Repeat) o;
            return Objects.equals(myMatchers, r.myMatchers) && Objects.equals(mySeparator, r.mySeparator);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myMatchers, mySeparator);
        }
    }

    public static class InvalidPatternException extends Exception {
        public InvalidPatternException() {
            super();
        }
    }

    @Nullable
    public static Matcher buildFor(@NotNull RsMacroDefinitionBase macro) {
        var body = macro.getMacroBodyStubbed();
        if (body == null) return null;

        List<Matcher> matchers = new ArrayList<>();

        for (var macroCase : body.getMacroCaseList()) {
            List<Matcher> subMatchers = new ArrayList<>();
            var contents = macroCase.getMacroPattern().getMacroPatternContents();
            MacroPattern macroPattern = MacroPattern.valueOf(contents);

            for (ASTNode psi : macroPattern.getPattern()) {
                try {
                    addNewMatcher(psi, subMatchers);
                } catch (InvalidPatternException e) {
                    return null;
                }
            }
            if (subMatchers.isEmpty()) {
                continue;
            } else if (subMatchers.size() == 1) {
                matchers.add(subMatchers.get(0));
            } else {
                matchers.add(new Sequence(subMatchers));
            }
        }

        return new Choice(matchers);
    }

    private static void addNewMatcher(@NotNull ASTNode node, @NotNull List<Matcher> matchers)
        throws InvalidPatternException {
        if (node.getElementType() == RsElementTypes.MACRO_BINDING) {
            RsMacroBinding psi = (RsMacroBinding) node.getPsi();
            String specifier = org.rust.lang.core.psi.ext.RsMacroBindingUtil.getFragmentSpecifier(psi);
            if (specifier == null) throw new InvalidPatternException();
            FragmentKind kind = FragmentKind.fromString(specifier);
            if (kind == null) throw new InvalidPatternException();
            matchers.add(new Fragment(kind));
        } else if (node.getElementType() == RsElementTypes.MACRO_BINDING_GROUP) {
            RsMacroBindingGroup psi = (RsMacroBindingGroup) node.getPsi();
            List<Matcher> subMatchers = new ArrayList<>();
            var contents = psi.getMacroPatternContents();
            if (contents == null) throw new InvalidPatternException();
            MacroPattern macroPattern = MacroPattern.valueOf(contents);
            for (ASTNode subPsi : macroPattern.getPattern()) {
                addNewMatcher(subPsi, subMatchers);
            }
            ASTNode separatorNode = psi.getMacroBindingGroupSeparator() != null
                ? psi.getMacroBindingGroupSeparator().getNode().getFirstChildNode()
                : null;
            Matcher matcher;
            if (psi.getMul() != null) {
                matcher = new Optional(new Repeat(subMatchers, separatorNode));
            } else if (psi.getPlus() != null) {
                matcher = new Repeat(subMatchers, separatorNode); // at least once
            } else if (psi.getQ() != null) {
                matcher = new Optional(new Sequence(subMatchers)); // at most once
            } else {
                throw new InvalidPatternException();
            }
            matchers.add(matcher);
        } else {
            matchers.add(new Literal(node));
        }
    }
}
