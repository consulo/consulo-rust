/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.decl;

import consulo.application.progress.ProgressManager;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.LighterASTNode;
import consulo.language.ast.TokenType;
import consulo.language.parser.PsiBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.errors.MacroMatchingError;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMacroBinding;
import org.rust.lang.core.psi.RsMacroBindingGroup;
import org.rust.lang.core.psi.RsMacroBindingGroupSeparator;
import org.rust.lang.core.psi.RsMacroPatternContents;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import org.rust.lang.core.psi.ext.impl.RsMacroBindingUtil;
import org.rust.lang.core.psi.impl.RsTokenSets;
import org.rust.stdext.RsResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The left-hand side of a {@code macro_rules!} case, flattened to its tokens, bindings ({@code $x:kind}) and
 * binding groups ({@code $(...)sep*}), matched against the body of a macro call.
 */
public class MacroPattern {
    @Nonnull
    private final List<ASTNode> myPattern;

    private MacroPattern(@Nonnull List<ASTNode> pattern) {
        myPattern = pattern;
    }

    @Nonnull
    public List<ASTNode> getPattern() {
        return myPattern;
    }

    public boolean isEmpty() {
        return myPattern.isEmpty();
    }

    /**
     * Matches the whole remaining input: a match that leaves tokens unconsumed is an error.
     */
    @Nonnull
    public RsResult<MacroSubstitution, MacroMatchingError> match(@Nonnull PsiBuilder macroCallBody) {
        RsResult<MacroSubstitution, MacroMatchingError> result = matchPartial(macroCallBody);
        if (result.isErr()) return result;
        if (!macroCallBody.eof()) {
            MacroExpansionMarks.FailMatchPatternByExtraInput.hit();
            return new RsResult.Err<>(new MacroMatchingError.ExtraInput(macroCallBody.getCurrentOffset()));
        }
        return result;
    }

    @Nonnull
    private RsResult<MacroSubstitution, MacroMatchingError> matchPartial(@Nonnull PsiBuilder macroCallBody) {
        ProgressManager.checkCanceled();
        Map<String, MetaVarValue> map = new HashMap<>();

        for (ASTNode node : myPattern) {
            IElementType type = node.getElementType();
            if (type == RsElementTypes.MACRO_BINDING) {
                RsMacroBinding psi = (RsMacroBinding) node.getPsi();
                String name = psi.getMetaVarIdentifier().getText();
                String fragmentSpecifier = RsMacroBindingUtil.getFragmentSpecifier(psi);
                FragmentKind kind = fragmentSpecifier != null ? FragmentKind.fromString(fragmentSpecifier) : null;
                if (name == null || kind == null) {
                    return new RsResult.Err<>(new MacroMatchingError.PatternSyntax(macroCallBody.getCurrentOffset()));
                }

                int lastOffset = macroCallBody.getCurrentOffset();
                boolean parsed = kind.parse(macroCallBody);
                if (!parsed || (lastOffset == macroCallBody.getCurrentOffset() && kind != FragmentKind.Vis)) {
                    MacroExpansionMarks.FailMatchPatternByBindingType.hit();
                    return new RsResult.Err<>(new MacroMatchingError.FragmentIsNotParsed(macroCallBody.getCurrentOffset(), name, kind));
                }
                String text = macroCallBody.getOriginalText().subSequence(lastOffset, macroCallBody.getCurrentOffset()).toString();
                LighterASTNode latestDone = macroCallBody.getLatestDoneMarker();
                IElementType elementType = latestDone != null ? latestDone.getTokenType() : null;
                map.put(name, new MetaVarValue.Fragment(text, kind, elementType, lastOffset));
            }
            else if (type == RsElementTypes.MACRO_BINDING_GROUP) {
                RsMacroBindingGroup psi = (RsMacroBindingGroup) node.getPsi();
                RsResult<List<MacroSubstitution>, MacroMatchingError> groupResult = matchGroup(psi, macroCallBody);
                if (groupResult.isErr()) return new RsResult.Err<>(groupResult.err());
                List<MacroSubstitution> nesteds = groupResult.ok();
                for (int i = 0; i < nesteds.size(); i++) {
                    for (Map.Entry<String, MetaVarValue> entry : nesteds.get(i).getVariables().entrySet()) {
                        MetaVarValue existing = map.computeIfAbsent(entry.getKey(), k -> new MetaVarValue.Group());
                        if (!(existing instanceof MetaVarValue.Group group)) {
                            return new RsResult.Err<>(new MacroMatchingError.Nesting(macroCallBody.getCurrentOffset(), entry.getKey()));
                        }
                        while (group.getNested().size() < i) {
                            group.getNested().add(new MetaVarValue.Group());
                        }
                        group.getNested().add(entry.getValue());
                    }
                }
                if (nesteds.isEmpty()) {
                    for (RsMacroBinding binding : PsiElementUtil.descendantsOfType(psi, RsMacroBinding.class)) {
                        String name = binding.getName();
                        if (name != null) {
                            map.put(name, MetaVarValue.EmptyGroup.INSTANCE);
                        }
                    }
                }
            }
            else if (!DeclMacroExpander.isSameToken(macroCallBody, node)) {
                MacroExpansionMarks.FailMatchPatternByToken.hit();
                IElementType actualToken = macroCallBody.getTokenType();
                String actualTokenText = macroCallBody.getTokenText();
                if (actualToken != null && actualTokenText != null) {
                    return new RsResult.Err<>(new MacroMatchingError.UnmatchedToken(
                        macroCallBody.getCurrentOffset(),
                        type.toString(),
                        node.getText(),
                        actualToken.toString(),
                        actualTokenText
                    ));
                }
                return new RsResult.Err<>(new MacroMatchingError.EndOfInput(macroCallBody.getCurrentOffset()));
            }
        }
        return new RsResult.Ok<>(new MacroSubstitution(map));
    }

    @Nonnull
    private static RsResult<List<MacroSubstitution>, MacroMatchingError> matchGroup(
        @Nonnull RsMacroBindingGroup group,
        @Nonnull PsiBuilder macroCallBody
    ) {
        List<MacroSubstitution> groups = new ArrayList<>();
        RsMacroPatternContents contents = group.getMacroPatternContents();
        if (contents == null) {
            return new RsResult.Err<>(new MacroMatchingError.PatternSyntax(macroCallBody.getCurrentOffset()));
        }
        MacroPattern pattern = valueOf(contents);
        if (pattern.isEmpty()) {
            return new RsResult.Err<>(new MacroMatchingError.PatternSyntax(macroCallBody.getCurrentOffset()));
        }
        RsMacroBindingGroupSeparator separatorPsi = group.getMacroBindingGroupSeparator();
        ASTNode separator = separatorPsi != null ? separatorPsi.getNode().getFirstChildNode() : null;
        macroCallBody.eof(); // skip whitespace
        PsiBuilder.Marker mark = macroCallBody.mark();

        while (true) {
            if (macroCallBody.eof()) {
                MacroExpansionMarks.GroupInputEnd1.hit();
                mark.rollbackTo();
                break;
            }

            int lastOffset = macroCallBody.getCurrentOffset();
            RsResult<MacroSubstitution, MacroMatchingError> result = pattern.matchPartial(macroCallBody);
            if (result.isOk()) {
                if (macroCallBody.getCurrentOffset() == lastOffset) {
                    MacroExpansionMarks.GroupMatchedEmptyTT.hit();
                    return new RsResult.Err<>(new MacroMatchingError.EmptyGroup(macroCallBody.getCurrentOffset()));
                }
                groups.add(result.ok());
            }
            else {
                MacroExpansionMarks.GroupInputEnd2.hit();
                mark.rollbackTo();
                break;
            }

            if (macroCallBody.eof()) {
                MacroExpansionMarks.GroupInputEnd3.hit();
                // a group was just matched completely: nothing to roll back
                mark.drop();
                break;
            }

            if (group.getQ() != null) {
                // `$(...)?` means "0 or 1 occurrences"
                MacroExpansionMarks.QuestionMarkGroupEnd.hit();
                mark.drop();
                break;
            }

            mark.drop();
            mark = macroCallBody.mark();

            if (separator != null && !DeclMacroExpander.isSameToken(macroCallBody, separator)) {
                MacroExpansionMarks.FailMatchGroupBySeparator.hit();
                mark.drop();
                break;
            }
        }

        if (group.getPlus() != null && groups.isEmpty()) {
            MacroExpansionMarks.FailMatchGroupTooFewElements.hit();
            return new RsResult.Err<>(new MacroMatchingError.TooFewGroupElements(macroCallBody.getCurrentOffset()));
        }

        return new RsResult.Ok<>(groups);
    }

    @Nonnull
    public static MacroPattern valueOf(@Nullable RsMacroPatternContents psi) {
        if (psi == null) return new MacroPattern(new ArrayList<>());
        return new MacroPattern(flattenChildren(psi.getNode()));
    }

    @Nonnull
    private static List<ASTNode> flattenChildren(@Nonnull ASTNode node) {
        List<ASTNode> result = new ArrayList<>();
        for (ASTNode child = node.getFirstChildNode(); child != null; child = child.getTreeNext()) {
            IElementType type = child.getElementType();
            if (type == TokenType.WHITE_SPACE || RsTokenSets.RS_COMMENTS.contains(type)) continue;
            if (type == RsElementTypes.MACRO_PATTERN || type == RsElementTypes.MACRO_PATTERN_CONTENTS) {
                result.addAll(flattenChildren(child));
            }
            else {
                result.add(child);
            }
        }
        return result;
    }
}
