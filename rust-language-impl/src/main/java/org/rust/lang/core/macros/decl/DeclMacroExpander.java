/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import consulo.language.ast.ASTNode;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderUtil;
import consulo.language.impl.parser.GeneratedParserUtilBase;
import consulo.project.Project;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.*;
import org.rust.lang.core.macros.errors.DeclMacroExpansionError;
import org.rust.lang.core.macros.errors.MacroMatchingError;
import org.rust.lang.core.parser.ParserUtil;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nullable;
import org.rust.lang.core.parser.RustParserUtil;
import org.rust.lang.core.psi.*;
import org.rust.stdext.RsResult;

import java.util.*;
import java.util.regex.Pattern;

import static org.rust.lang.core.psi.RsElementTypes.*;
import org.rust.lang.core.psi.impl.*;

/**
 * Declarative macro expander.
 */
public class DeclMacroExpander extends MacroExpander<RsDeclMacroData, DeclMacroExpansionError> {

    public static final int EXPANDER_VERSION = 20;

    public static final String MACRO_DOLLAR_CRATE_IDENTIFIER = "IntellijRustDollarCrate";
    public static final Pattern MACRO_DOLLAR_CRATE_IDENTIFIER_REGEX = Pattern.compile(MACRO_DOLLAR_CRATE_IDENTIFIER);

    private static final TokenSet USELESS_PARENS_EXPRS = RsTokenType.tokenSetOf(
        LIT_EXPR, MACRO_EXPR, PATH_EXPR, PAREN_EXPR, TUPLE_EXPR, ARRAY_EXPR, UNIT_EXPR, BLOCK_EXPR
    );

    /**
     * An expansion longer than this is not produced.
     */
    private static final int LARGE_EXPANSION = 20 * 1024 * 1024;

    @Nonnull
    private final Project myProject;

    public DeclMacroExpander(@Nonnull Project project) {
        myProject = project;
    }

    @Nonnull
    @Override
    public RsResult<Pair<CharSequence, RangeMap>, DeclMacroExpansionError> expandMacroAsTextWithErr(
        @Nonnull RsDeclMacroData def,
        @Nonnull RsMacroCallData call
    ) {
        RsResult<MatchedPattern, DeclMacroExpansionError> matched = findMatchingPattern(def, call);
        if (matched.isErr()) return new RsResult.Err<>(matched.err());
        MatchedPattern pattern = matched.ok();
        RsMacroExpansion expansion = pattern.macroCase().getMacroExpansion();
        RsMacroExpansionContents contents = expansion != null ? expansion.getMacroExpansionContents() : null;
        if (contents == null) return new RsResult.Err<>(DeclMacroExpansionError.DefSyntax);

        Map<String, MetaVarValue> variables = new HashMap<>(pattern.subst().getVariables());
        variables.put("crate", new MetaVarValue.Fragment(MACRO_DOLLAR_CRATE_IDENTIFIER, null, null, -1));

        RsResult<Pair<CharSequence, RangeMap>, DeclMacroExpansionError> result =
            substituteMacro(contents, new MacroSubstitution(variables));
        if (result.isErr()) return result;
        return new RsResult.Ok<>(Pair.create(result.ok().getFirst(), pattern.ranges().mapAll(result.ok().getSecond())));
    }

    @Nonnull
    private RsResult<MatchedPattern, DeclMacroExpansionError> findMatchingPattern(
        @Nonnull RsDeclMacroData def,
        @Nonnull RsMacroCallData call
    ) {
        if (!(call.getMacroBody() instanceof MacroCallBody.FunctionLike functionLike)) {
            return new RsResult.Err<>(DeclMacroExpansionError.DefSyntax);
        }
        Pair<PsiBuilder, RangeMap> lowered = DocsLowering.lowerDocCommentsToAdaptedPsiBuilder(
            ParserUtil.createAdaptedRustPsiBuilder(myProject, functionLike.getText()),
            myProject
        );
        PsiBuilder macroCallBody = lowered.getFirst();
        macroCallBody.eof(); // skip whitespace
        PsiBuilder.Marker start = macroCallBody.mark();
        RsMacroBody macroBody = def.getMacroBody().getValue();
        if (macroBody == null) return new RsResult.Err<>(DeclMacroExpansionError.DefSyntax);

        List<MacroMatchingError> errors = new ArrayList<>();
        for (RsMacroCase macroCase : macroBody.getMacroCaseList()) {
            RsResult<MacroSubstitution, MacroMatchingError> result =
                MacroPattern.valueOf(macroCase.getMacroPattern().getMacroPatternContents()).match(macroCallBody);
            if (result.isOk()) {
                return new RsResult.Ok<>(new MatchedPattern(macroCase, result.ok(), lowered.getSecond()));
            }
            errors.add(result.err());
            start.rollbackTo();
            start = macroCallBody.mark();
        }
        return new RsResult.Err<>(new DeclMacroExpansionError.Matching(errors));
    }

    private record MatchedPattern(@Nonnull RsMacroCase macroCase, @Nonnull MacroSubstitution subst, @Nonnull RangeMap ranges) {
    }

    /**
     * The expansion text being built. Appending keeps tokens apart with a space unless they were adjacent in the
     * macro call body.
     */
    private static final class MacroStringBuilder {
        private final StringBuilder myInner = new StringBuilder();
        @Nullable
        private MetaVarValue.Fragment myLastFragment;

        int length() {
            return myInner.length();
        }

        void append(@Nonnull String s) {
            if (s.isEmpty()) return;
            myInner.append(s);
            myLastFragment = null;
        }

        /**
         * Ensures that the buffer ends (or {@code str} starts) with a whitespace and appends {@code str}.
         */
        void safeAppend(@Nonnull CharSequence str) {
            if (myInner.length() > 0 && !Character.isWhitespace(myInner.charAt(myInner.length() - 1))
                && str.length() > 0 && !Character.isWhitespace(str.charAt(0))) {
                myInner.append(' ');
            }
            myInner.append(str);
            myLastFragment = null;
        }

        void safeAppend(@Nonnull MetaVarValue.Fragment fragment) {
            MetaVarValue.Fragment last = myLastFragment;
            if (last != null && last.getOffsetInCallBody() + last.getValue().length() == fragment.getOffsetInCallBody()) {
                // the fragments were adjacent in the macro call body
                myInner.append(fragment.getValue());
            }
            else {
                safeAppend(fragment.getValue());
            }
            myLastFragment = fragment;
        }

        void delete(int start, int end) {
            if (start == end) return;
            myInner.delete(start, end);
            myLastFragment = null;
        }
    }

    @Nonnull
    private static RsResult<Pair<CharSequence, RangeMap>, DeclMacroExpansionError> substituteMacro(
        @Nonnull PsiElement root,
        @Nonnull MacroSubstitution subst
    ) {
        MacroStringBuilder sb = new MacroStringBuilder();
        List<MappedTextRange> ranges = new ArrayList<>();
        if (!substituteMacro(sb, ranges, root.getNode(), subst, new ArrayList<>())) {
            if (sb.length() > LARGE_EXPANSION) {
                return new RsResult.Err<>(DeclMacroExpansionError.TooLargeExpansion);
            }
            return new RsResult.Err<>(DeclMacroExpansionError.DefSyntax);
        }
        return new RsResult.Ok<>(Pair.create(sb.myInner, new RangeMap(ranges)));
    }

    private static boolean substituteMacro(
        @Nonnull MacroStringBuilder sb,
        @Nonnull List<MappedTextRange> ranges,
        @Nonnull ASTNode root,
        @Nonnull MacroSubstitution subst,
        @Nonnull List<NestingState> nesting
    ) {
        if (sb.length() > LARGE_EXPANSION) return false;

        for (ASTNode child = root.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType type = child.getElementType();
            if (RsTokenSets.RS_REGULAR_COMMENTS.contains(type)) {
                continue;
            }
            if (type == MACRO_EXPANSION || type == MACRO_EXPANSION_CONTENTS) {
                if (!substituteMacro(sb, ranges, child, subst, nesting)) return false;
            }
            else if (type == MACRO_REFERENCE) {
                String name = ((RsMacroReference) child.getPsi()).getReferenceName();
                MetaVarValue value = lookup(subst, name, nesting);
                if (value == null) {
                    MacroExpansionMarks.SubstMetaVarNotFound.hit();
                    sb.safeAppend(child.getText());
                    continue;
                }
                if (!(value instanceof MetaVarValue.Fragment fragment)) {
                    return true;
                }
                boolean parensNeeded = fragment.getKind() == FragmentKind.Expr
                    && !USELESS_PARENS_EXPRS.contains(fragment.getElementType());
                if (parensNeeded) {
                    sb.append("(");
                    sb.append(fragment.getValue());
                    sb.append(")");
                }
                else {
                    sb.safeAppend(fragment);
                }
                if (fragment.getOffsetInCallBody() != -1 && !fragment.getValue().isEmpty()) {
                    RangeMap.mergeAdd(ranges, new MappedTextRange(
                        fragment.getOffsetInCallBody(),
                        sb.length() - fragment.getValue().length() - (parensNeeded ? 1 : 0),
                        fragment.getValue().length()
                    ));
                }
            }
            else if (type == MACRO_EXPANSION_REFERENCE_GROUP) {
                RsMacroExpansionReferenceGroup group = (RsMacroExpansionReferenceGroup) child.getPsi();
                RsMacroExpansionContents contents = group.getMacroExpansionContents();
                if (contents == null) continue;
                NestingState state = new NestingState();
                nesting.add(state);
                RsMacroExpansionGroupSeparator separatorPsi = group.getMacroExpansionGroupSeparator();
                String separator = separatorPsi != null ? separatorPsi.getText() : "";

                for (int i = 0; i < 65536; i++) {
                    int lastPosition = sb.length();
                    if (!substituteMacro(sb, ranges, contents.getNode(), subst, nesting)) return false;
                    if (state.atTheEnd || !state.hit) {
                        sb.delete(lastPosition, sb.length());
                        if (i != 0) {
                            sb.delete(sb.length() - separator.length(), sb.length());
                        }
                        while (!ranges.isEmpty() && ranges.get(ranges.size() - 1).getDstOffset() >= sb.length()) {
                            ranges.remove(ranges.size() - 1);
                        }
                        break;
                    }
                    state.idx++;
                    state.hit = false;
                    sb.append(separator);
                }

                nesting.remove(nesting.size() - 1);
            }
            else {
                sb.safeAppend(child.getChars());
            }
        }
        return true;
    }

    /**
     * The value of {@code $name} at the current repetition indices, {@code null} if there is no such meta variable,
     * or a non-{@link MetaVarValue.Fragment fragment} when the repetition is exhausted (which ends the group).
     */
    @Nullable
    private static MetaVarValue lookup(@Nonnull MacroSubstitution subst, @Nonnull String name, @Nonnull List<NestingState> nesting) {
        MetaVarValue value = subst.getVariables().get(name);
        if (value == null) return null;
        for (NestingState state : nesting) {
            state.hit = true;
            if (value instanceof MetaVarValue.Fragment) {
                break;
            }
            if (value instanceof MetaVarValue.Group group && state.idx < group.getNested().size()) {
                value = group.getNested().get(state.idx);
            }
            else {
                state.atTheEnd = true;
                return MetaVarValue.EmptyGroup.INSTANCE;
            }
        }
        return value instanceof MetaVarValue.Fragment ? value : MetaVarValue.EmptyGroup.INSTANCE;
    }

    private static final class NestingState {
        int idx;
        boolean hit;
        boolean atTheEnd;
    }

    /**
     * Checks whether the given PsiBuilder token matches the given ASTNode.
     */
    public static boolean isSameToken(@Nonnull PsiBuilder builder, @Nonnull ASTNode node) {
        Object[] collapsed = RustParserUtil.collapsedTokenType(builder);
        IElementType elementType;
        int size;
        if (collapsed != null) {
            elementType = (IElementType) collapsed[0];
            size = (Integer) collapsed[1];
        } else {
            elementType = builder.getTokenType();
            size = 1;
        }
        TokenSet compareByTextTokens = TokenSet.orSet(
            RsTokenType.tokenSetOf(IDENTIFIER, QUOTE_IDENTIFIER),
            RsTokenSets.RS_LITERALS
        );
        boolean result = node.getElementType() == elementType
            && (elementType == null || !compareByTextTokens.contains(elementType)
            || GeneratedParserUtilBase.nextTokenIsFast(builder, node.getText(), true) == size);
        if (result) {
            PsiBuilderUtil.advance(builder, size);
        }
        return result;
    }
}
