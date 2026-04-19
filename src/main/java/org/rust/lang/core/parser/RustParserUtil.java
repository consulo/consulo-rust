/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.parser;

import com.intellij.lang.LighterASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderUtil;
import com.intellij.lang.WhitespacesAndCommentsBinder;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.util.Key;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.BitUtil;
import com.intellij.lexer.Lexer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.MacroBraces;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.stdext.CollectionsUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.RsTokenType.*;

@SuppressWarnings("unused")
public class RustParserUtil extends GeneratedParserUtilBase {

    public static final RustParserUtil INSTANCE = new RustParserUtil();

    // Enums

    public enum PathParsingMode {
        /** Accepts paths like {@code Foo::<i32>}. Should be used to parse references to values. */
        VALUE,
        /** Accepts paths like {@code Foo<i32>}, {@code Foo::<i32>}, {@code Fn(i32) -> i32} and {@code Fn::(i32) -> i32}. */
        TYPE,
        /** Accepts paths like {@code Foo}. Should be used where type args cannot be specified. */
        NO_TYPE_ARGS
    }

    public enum TypeQualsMode { ON, OFF }
    public enum StructLiteralsMode { ON, OFF }
    public enum StmtMode { ON, OFF }
    public enum RestrictedConstExprMode { ON, OFF }
    public enum ConditionMode { ON, OFF }

    public enum MacroCallParsingMode {
        ITEM(false, true, true, SpecialMacroParsingMode.PARSE_AS_SPECIAL),
        BLOCK(true, true, false, SpecialMacroParsingMode.FORBID),
        EXPR(true, false, true, SpecialMacroParsingMode.PARSE_AS_SPECIAL),
        META(false, false, false, SpecialMacroParsingMode.PARSE_AS_USUAL);

        public final boolean attrsAndVis;
        public final boolean semicolon;
        public final boolean pin;
        public final SpecialMacroParsingMode specialMacros;

        MacroCallParsingMode(boolean attrsAndVis, boolean semicolon, boolean pin, SpecialMacroParsingMode specialMacros) {
            this.attrsAndVis = attrsAndVis;
            this.semicolon = semicolon;
            this.pin = pin;
            this.specialMacros = specialMacros;
        }
    }

    public enum SpecialMacroParsingMode { FORBID, PARSE_AS_SPECIAL, PARSE_AS_USUAL }

    // Flags
    private static final Key<Integer> FLAGS = Key.create("RustParserUtil.FLAGS");
    private static final Key<com.intellij.util.containers.Stack<Integer>> FLAG_STACK = Key.create("RustParserUtil.FLAG_STACK");

    private static int getFlags(@NotNull PsiBuilder b) {
        Integer flags = b.getUserData(FLAGS);
        return flags != null ? flags : DEFAULT_FLAGS;
    }

    private static void setFlags(@NotNull PsiBuilder b, int value) {
        b.putUserData(FLAGS, value);
    }

    @NotNull
    private static com.intellij.util.containers.Stack<Integer> getFlagStack(@NotNull PsiBuilder b) {
        com.intellij.util.containers.Stack<Integer> stack = b.getUserData(FLAG_STACK);
        return stack != null ? stack : new com.intellij.util.containers.Stack<>(0);
    }

    private static void setFlagStack(@NotNull PsiBuilder b, @NotNull com.intellij.util.containers.Stack<Integer> stack) {
        b.putUserData(FLAG_STACK, stack);
    }

    private static void pushFlag(@NotNull PsiBuilder b, int flag, boolean mode) {
        com.intellij.util.containers.Stack<Integer> stack = getFlagStack(b);
        stack.push(getFlags(b));
        setFlagStack(b, stack);
        setFlags(b, BitUtil.set(getFlags(b), flag, mode));
    }

    private static void pushFlags(@NotNull PsiBuilder b, int[] flags, boolean[] values) {
        com.intellij.util.containers.Stack<Integer> stack = getFlagStack(b);
        stack.push(getFlags(b));
        setFlagStack(b, stack);
        int currentFlags = getFlags(b);
        for (int i = 0; i < flags.length; i++) {
            currentFlags = BitUtil.set(currentFlags, flags[i], values[i]);
        }
        setFlags(b, currentFlags);
    }

    private static void popFlag(@NotNull PsiBuilder b) {
        setFlags(b, getFlagStack(b).pop());
    }

    private static int setFlagValue(int flags, int flag, boolean mode) {
        return BitUtil.set(flags, flag, mode);
    }

    private static final int STRUCT_ALLOWED = CollectionsUtil.makeBitMask(1);
    private static final int TYPE_QUAL_ALLOWED = CollectionsUtil.makeBitMask(2);
    private static final int STMT_EXPR_MODE = CollectionsUtil.makeBitMask(3);
    private static final int PATH_MODE_VALUE = CollectionsUtil.makeBitMask(4);
    private static final int PATH_MODE_TYPE = CollectionsUtil.makeBitMask(5);
    private static final int PATH_MODE_NO_TYPE_ARGS = CollectionsUtil.makeBitMask(6);
    private static final int MACRO_BRACE_PARENS = CollectionsUtil.makeBitMask(7);
    private static final int MACRO_BRACE_BRACKS = CollectionsUtil.makeBitMask(8);
    private static final int MACRO_BRACE_BRACES = CollectionsUtil.makeBitMask(9);
    private static final int RESTRICTED_CONST_EXPR_MODE = CollectionsUtil.makeBitMask(10);
    private static final int CONDITION_MODE = CollectionsUtil.makeBitMask(11);

    private static int setPathMod(int flags, @NotNull PathParsingMode mode) {
        int flag;
        switch (mode) {
            case VALUE: flag = PATH_MODE_VALUE; break;
            case TYPE: flag = PATH_MODE_TYPE; break;
            case NO_TYPE_ARGS: flag = PATH_MODE_NO_TYPE_ARGS; break;
            default: throw new IllegalArgumentException();
        }
        return (flags & ~(PATH_MODE_VALUE | PATH_MODE_TYPE | PATH_MODE_NO_TYPE_ARGS)) | flag;
    }

    @NotNull
    private static PathParsingMode getPathMod(int flags) {
        if (BitUtil.isSet(flags, PATH_MODE_VALUE)) return PathParsingMode.VALUE;
        if (BitUtil.isSet(flags, PATH_MODE_TYPE)) return PathParsingMode.TYPE;
        if (BitUtil.isSet(flags, PATH_MODE_NO_TYPE_ARGS)) return PathParsingMode.NO_TYPE_ARGS;
        throw new IllegalStateException("Path parsing mode not set");
    }

    private static int setMacroBraces(int flags, @NotNull MacroBraces mode) {
        int flag;
        switch (mode) {
            case PARENS: flag = MACRO_BRACE_PARENS; break;
            case BRACKS: flag = MACRO_BRACE_BRACKS; break;
            case BRACES: flag = MACRO_BRACE_BRACES; break;
            default: throw new IllegalArgumentException();
        }
        return (flags & ~(MACRO_BRACE_PARENS | MACRO_BRACE_BRACKS | MACRO_BRACE_BRACES)) | flag;
    }

    @Nullable
    private static MacroBraces getMacroBraces(int flags) {
        if (BitUtil.isSet(flags, MACRO_BRACE_PARENS)) return MacroBraces.PARENS;
        if (BitUtil.isSet(flags, MACRO_BRACE_BRACKS)) return MacroBraces.BRACKS;
        if (BitUtil.isSet(flags, MACRO_BRACE_BRACES)) return MacroBraces.BRACES;
        return null;
    }

    private static final int DEFAULT_FLAGS = STRUCT_ALLOWED | TYPE_QUAL_ALLOWED;

    @NotNull
    public static final WhitespacesAndCommentsBinder ADJACENT_LINE_COMMENTS = (tokens, atStreamEdge, getter) -> {
        int candidate = tokens.size();
        for (int i = 0; i < tokens.size(); i++) {
            IElementType token = tokens.get(i);
            if (RustParserDefinition.OUTER_BLOCK_DOC_COMMENT == token || RustParserDefinition.OUTER_EOL_DOC_COMMENT == token) {
                candidate = Math.min(candidate, i);
                break;
            }
            if (RustParserDefinition.EOL_COMMENT == token) {
                candidate = Math.min(candidate, i);
            }
            if (TokenType.WHITE_SPACE == token && getter.get(i).toString().contains("\n\n")) {
                candidate = tokens.size();
            }
        }
        return candidate;
    };

    private static final TokenSet LEFT_BRACES = tokenSetOf(LPAREN, LBRACE, LBRACK);
    private static final TokenSet RIGHT_BRACES = tokenSetOf(RPAREN, RBRACE, RBRACK);

    //
    // Helpers
    //

    @SuppressWarnings("unused")
    public static boolean checkStructAllowed(@NotNull PsiBuilder b, int level) {
        return BitUtil.isSet(getFlags(b), STRUCT_ALLOWED);
    }

    @SuppressWarnings("unused")
    public static boolean checkTypeQualAllowed(@NotNull PsiBuilder b, int level) {
        return BitUtil.isSet(getFlags(b), TYPE_QUAL_ALLOWED);
    }

    @SuppressWarnings("unused")
    public static boolean checkBraceAllowed(@NotNull PsiBuilder b, int level) {
        return b.getTokenType() != LBRACE || checkStructAllowed(b, level);
    }

    @SuppressWarnings("unused")
    public static boolean checkGtAllowed(@NotNull PsiBuilder b, int level) {
        return !BitUtil.isSet(getFlags(b), RESTRICTED_CONST_EXPR_MODE);
    }

    @SuppressWarnings("unused")
    public static boolean checkLetExprAllowed(@NotNull PsiBuilder b, int level) {
        return BitUtil.isSet(getFlags(b), CONDITION_MODE);
    }

    @SuppressWarnings("unused")
    public static boolean withRestrictedConstExprMode(@NotNull PsiBuilder b, int level, @NotNull RestrictedConstExprMode mode, @NotNull Parser parser) {
        int oldFlags = getFlags(b);
        int newFlags = setFlagValue(oldFlags, RESTRICTED_CONST_EXPR_MODE, mode == RestrictedConstExprMode.ON);
        setFlags(b, newFlags);
        boolean result = parser.parse(b, level);
        setFlags(b, oldFlags);
        return result;
    }

    @SuppressWarnings("unused")
    public static boolean withConditionMode(@NotNull PsiBuilder b, int level, @NotNull ConditionMode mode, @NotNull Parser parser) {
        int oldFlags = getFlags(b);
        int newFlags = setFlagValue(oldFlags, CONDITION_MODE, mode == ConditionMode.ON);
        setFlags(b, newFlags);
        boolean result = parser.parse(b, level);
        setFlags(b, oldFlags);
        return result;
    }

    @SuppressWarnings("unused")
    public static boolean enterBlockExpr(@NotNull PsiBuilder b, int level, @NotNull Parser parser) {
        int oldFlags = getFlags(b);
        int newFlags = setFlagValue(setFlagValue(oldFlags, RESTRICTED_CONST_EXPR_MODE, false), CONDITION_MODE, false);
        setFlags(b, newFlags);
        boolean result = parser.parse(b, level);
        setFlags(b, oldFlags);
        return result;
    }

    @SuppressWarnings("unused")
    public static boolean setStmtMode(@NotNull PsiBuilder b, int level, @NotNull StmtMode mode) {
        pushFlag(b, STMT_EXPR_MODE, mode == StmtMode.ON);
        return true;
    }

    @SuppressWarnings("unused")
    public static boolean setLambdaExprMode(@NotNull PsiBuilder b, int level) {
        pushFlags(b, new int[]{STMT_EXPR_MODE, CONDITION_MODE}, new boolean[]{false, false});
        return true;
    }

    @SuppressWarnings("unused")
    public static boolean resetFlags(@NotNull PsiBuilder b, int level) {
        popFlag(b);
        return true;
    }

    @SuppressWarnings("unused")
    public static boolean exprMode(
        @NotNull PsiBuilder b,
        int level,
        @NotNull StructLiteralsMode structLiterals,
        @NotNull StmtMode stmtMode,
        @NotNull Parser parser
    ) {
        int oldFlags = getFlags(b);
        int newFlags = setFlagValue(
            setFlagValue(oldFlags, STRUCT_ALLOWED, structLiterals == StructLiteralsMode.ON),
            STMT_EXPR_MODE, stmtMode == StmtMode.ON
        );
        setFlags(b, newFlags);
        boolean result = parser.parse(b, level);
        setFlags(b, oldFlags);
        return result;
    }

    @SuppressWarnings("unused")
    public static boolean isCompleteBlockExpr(@NotNull PsiBuilder b, int level) {
        return isBlock(b, level) && BitUtil.isSet(getFlags(b), STMT_EXPR_MODE);
    }

    @SuppressWarnings("unused")
    public static boolean isIncompleteBlockExpr(@NotNull PsiBuilder b, int level) {
        return !isCompleteBlockExpr(b, level);
    }

    @SuppressWarnings("unused")
    public static boolean isBlock(@NotNull PsiBuilder b, int level) {
        LighterASTNode m = b.getLatestDoneMarker();
        if (m == null) return false;
        return RS_BLOCK_LIKE_EXPRESSIONS.contains(m.getTokenType()) || isBracedMacro(m, b);
    }

    @SuppressWarnings("unused")
    public static boolean pathMode(@NotNull PsiBuilder b, int level, @NotNull PathParsingMode mode, @NotNull TypeQualsMode typeQualsMode, @NotNull Parser parser) {
        int oldFlags = getFlags(b);
        int newFlags = setPathMod(BitUtil.set(oldFlags, TYPE_QUAL_ALLOWED, typeQualsMode == TypeQualsMode.ON), mode);
        setFlags(b, newFlags);

        // A hack that reduces the growth rate of `level`. This actually allows a deeper path nesting.
        Frame prevPathFrame = null;
        Frame currentFrame = ErrorState.get(b).currentFrame;
        if (currentFrame != null && currentFrame.parentFrame != null) {
            prevPathFrame = ancestorOfTypeOrSelf(currentFrame.parentFrame, PATH);
        }
        int nextLevel;
        if (prevPathFrame != null) {
            nextLevel = Math.max(prevPathFrame.level + 2, level - 9);
        } else {
            nextLevel = level;
        }

        boolean result = parser.parse(b, nextLevel);
        setFlags(b, oldFlags);
        return result;
    }

    @SuppressWarnings("unused")
    public static boolean isPathMode(@NotNull PsiBuilder b, int level, @NotNull PathParsingMode mode) {
        return mode == getPathMod(getFlags(b));
    }

    /**
     * {@code FLOAT_LITERAL} is never produced during lexing. We construct it during parsing from one or
     * several {@code INTEGER_LITERAL} tokens.
     */
    @SuppressWarnings("unused")
    public static boolean parseFloatLiteral(@NotNull PsiBuilder b, int level) {
        IElementType tokenType = b.getTokenType();
        if (tokenType == INTEGER_LITERAL) {
            if (b.rawLookup(1) == DOT) {
                IElementType afterDot = b.rawLookup(2);
                boolean collapse;
                int size;
                if (afterDot == INTEGER_LITERAL || afterDot == FLOAT_LITERAL) {
                    collapse = true;
                    size = 3;
                } else if (afterDot == IDENTIFIER) {
                    collapse = false;
                    size = 0;
                } else {
                    collapse = true;
                    size = 2;
                }
                if (collapse) {
                    PsiBuilder.Marker marker = b.mark();
                    PsiBuilderUtil.advance(b, size);
                    marker.collapse(FLOAT_LITERAL);
                    return true;
                }
            }
            // Works with floats without `.` like `1f32`, `1e3`, `3e-4`
            String text = b.getTokenText();
            boolean isFloat = text != null
                && (text.contains("f") || (text.toLowerCase().contains("e") && !text.endsWith("e")))
                && !text.startsWith("0x");
            if (isFloat) {
                b.remapCurrentToken(FLOAT_LITERAL);
                b.advanceLexer();
            }
            return isFloat;
        } else if (tokenType == FLOAT_LITERAL) {
            b.advanceLexer();
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    public static boolean parsePathIdent(@NotNull PsiBuilder b, int level) {
        IElementType tokenType = b.getTokenType();
        boolean result;
        if (tokenType == SELF || tokenType == SUPER || tokenType == CSELF || tokenType == CRATE) {
            result = true;
        } else if (tokenType == IDENTIFIER) {
            String tokenText = b.getTokenText();
            result = !("union".equals(tokenText) && b.lookAhead(1) == IDENTIFIER
                || "async".equals(tokenText) && b.lookAhead(1) == FN);
        } else {
            // error message
            boolean consumed = consumeToken(b, IDENTIFIER);
            assert !consumed;
            result = false;
        }

        if (result) {
            consumeToken(b, tokenType);
        }
        return result;
    }

    @SuppressWarnings("unused")
    public static boolean unpairedToken(@NotNull PsiBuilder b, int level) {
        IElementType tokenType = b.getTokenType();
        if (tokenType == LBRACE || tokenType == RBRACE
            || tokenType == LPAREN || tokenType == RPAREN
            || tokenType == LBRACK || tokenType == RBRACK) {
            return false;
        }
        if (tokenType == null) {
            return false; // EOF
        }

        IElementType collapsedType = null;
        int collapsedSize = 0;
        Object collapsed = collapsedTokenType(b);
        if (collapsed != null) {
            collapsedType = ((IElementType[]) collapsed)[0];
            collapsedSize = ((int[]) ((Object[]) collapsed)[1])[0];
        }

        if (collapsedType != null && collapsedSize > 0) {
            PsiBuilder.Marker marker = b.mark();
            PsiBuilderUtil.advance(b, collapsedSize);
            marker.collapse(collapsedType);
        } else {
            b.advanceLexer();
        }
        return true;
    }

    /**
     * Returns collapsed token type and size as a 2-element array [IElementType, Integer], or null.
     */
    @Nullable
    private static Object[] collapsedTokenTypeResult(@NotNull PsiBuilder b) {
        // Re-use the public collapsedTokenType method logic
        // but return as array for internal use
        return null; // handled by the public method below
    }

    @SuppressWarnings("unused")
    public static boolean macroBindingGroupSeparatorToken(@NotNull PsiBuilder b, int level) {
        IElementType tokenType = b.getTokenType();
        if (tokenType == PLUS || tokenType == MUL || tokenType == Q) {
            return false;
        }
        return unpairedToken(b, level);
    }

    @SuppressWarnings("unused")
    public static boolean macroSemicolon(@NotNull PsiBuilder b, int level) {
        LighterASTNode m = b.getLatestDoneMarker();
        if (m == null) return false;
        b.getTokenText();
        if (b.getOriginalText().charAt(m.getEndOffset() - getBuilderOffset(b) - 1) == '}') return true;
        return consumeToken(b, SEMICOLON);
    }

    @SuppressWarnings("unused")
    public static boolean macroIdentifier(@NotNull PsiBuilder b, int level) {
        IElementType tokenType = b.getTokenType();
        if (tokenType == IDENTIFIER || RS_KEYWORDS.contains(tokenType) || tokenType == BOOL_LITERAL) {
            b.advanceLexer();
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    public static boolean pathOrTraitType(
        @NotNull PsiBuilder b,
        int level,
        @NotNull Parser pathP,
        @NotNull Parser implicitTraitTypeP,
        @NotNull Parser traitTypeUpperP
    ) {
        PsiBuilder.Marker pathOrTraitType = enter_section_(b);
        PsiBuilder.Marker polybound = enter_section_(b);
        PsiBuilder.Marker bound = enter_section_(b);
        PsiBuilder.Marker traitRef = enter_section_(b);

        if (!pathP.parse(b, level) || nextTokenIs(b, EXCL)) {
            exit_section_(b, traitRef, null, false);
            exit_section_(b, bound, null, false);
            exit_section_(b, polybound, null, false);
            exit_section_(b, pathOrTraitType, null, false);
            return implicitTraitTypeP.parse(b, level);
        }

        if (!nextTokenIs(b, PLUS)) {
            exit_section_(b, traitRef, null, true);
            exit_section_(b, bound, null, true);
            exit_section_(b, polybound, null, true);
            exit_section_(b, pathOrTraitType, PATH_TYPE, true);
            return true;
        }

        exit_section_(b, traitRef, TRAIT_REF, true);
        exit_section_(b, bound, BOUND, true);
        exit_section_(b, polybound, POLYBOUND, true);
        boolean result = traitTypeUpperP.parse(b, level);
        exit_section_(b, pathOrTraitType, TRAIT_TYPE, result);
        return result;
    }

    @SuppressWarnings({"DuplicatedCode", "unused"})
    public static boolean typeReferenceOrAssocTypeBinding(
        @NotNull PsiBuilder b,
        int level,
        @NotNull Parser pathP,
        @NotNull Parser assocTypeBindingUpperP,
        @NotNull Parser typeReferenceP,
        @NotNull Parser traitTypeUpperP
    ) {
        if (b.getTokenType() == DYN || (b.getTokenType() == IDENTIFIER && "dyn".equals(b.getTokenText()) && b.lookAhead(1) != EXCL)) {
            return typeReferenceP.parse(b, level);
        }

        PsiBuilder.Marker typeOrAssoc = enter_section_(b);
        PsiBuilder.Marker polybound = enter_section_(b);
        PsiBuilder.Marker bound = enter_section_(b);
        PsiBuilder.Marker traitRef = enter_section_(b);

        if (!pathP.parse(b, level) || nextTokenIsFast(b, EXCL)) {
            exit_section_(b, traitRef, null, false);
            exit_section_(b, bound, null, false);
            exit_section_(b, polybound, null, false);
            exit_section_(b, typeOrAssoc, null, false);
            return typeReferenceP.parse(b, level);
        }

        if (nextTokenIsFast(b, PLUS)) {
            exit_section_(b, traitRef, TRAIT_REF, true);
            exit_section_(b, bound, BOUND, true);
            exit_section_(b, polybound, POLYBOUND, true);
            boolean result = traitTypeUpperP.parse(b, level);
            exit_section_(b, typeOrAssoc, TRAIT_TYPE, result);
            return result;
        }

        exit_section_(b, traitRef, null, true);
        exit_section_(b, bound, null, true);
        exit_section_(b, polybound, null, true);

        if (!nextTokenIsFast(b, EQ) && !nextTokenIsFast(b, COLON)) {
            exit_section_(b, typeOrAssoc, PATH_TYPE, true);
            return true;
        }

        boolean result = assocTypeBindingUpperP.parse(b, level);
        exit_section_(b, typeOrAssoc, ASSOC_TYPE_BINDING, result);
        return result;
    }

    // Special macro parsers

    private static final Map<String, SpecialMacroParser> SPECIAL_MACRO_PARSERS = new HashMap<>();
    private static final Set<String> SPECIAL_EXPR_MACROS = new HashSet<>();

    @FunctionalInterface
    private interface SpecialMacroParser {
        boolean parse(@NotNull PsiBuilder b, int level);
    }

    static {
        putSpecialMacro(RustParser::ExprMacroArgument, true, "dbg");
        putSpecialMacro(RustParser::FormatMacroArgument, true,
            "format", "format_args", "format_args_nl", "write", "writeln",
            "print", "println", "eprint", "eprintln", "panic", "unimplemented", "unreachable", "todo");
        putSpecialMacro(RustParser::AssertMacroArgument, true,
            "assert", "debug_assert", "assert_eq", "assert_ne",
            "debug_assert_eq", "debug_assert_ne");
        putSpecialMacro(RustParser::VecMacroArgument, true, "vec");
        putSpecialMacro(RustParser::IncludeMacroArgument, true, "include_str", "include_bytes");
        putSpecialMacro(RustParser::IncludeMacroArgument, false, "include");
        putSpecialMacro(RustParser::ConcatMacroArgument, true, "concat");
        putSpecialMacro(RustParser::EnvMacroArgument, true, "env");
        putSpecialMacro(RustParser::AsmMacroArgument, true, "asm");
    }

    private static void putSpecialMacro(@NotNull SpecialMacroParser parser, boolean isExpr, @NotNull String... keys) {
        for (String name : keys) {
            if (SPECIAL_MACRO_PARSERS.containsKey(name)) {
                throw new IllegalStateException(name + " was already added");
            }
            SPECIAL_MACRO_PARSERS.put(name, parser);
            if (isExpr) {
                SPECIAL_EXPR_MACROS.add(name);
            }
        }
    }

    public static boolean isSpecialMacro(@NotNull String name) {
        return SPECIAL_MACRO_PARSERS.containsKey(name);
    }

    @SuppressWarnings("unused")
    public static boolean parseMacroCall(@NotNull PsiBuilder b, int level, @NotNull MacroCallParsingMode mode) {
        if (mode.attrsAndVis && !RustParser.AttrsAndVis(b, level + 1)) return false;

        if (!RustParser.PathWithoutTypeArgs(b, level + 1) || !consumeToken(b, EXCL)) {
            return false;
        }

        String macroName = getMacroName(b, -2);
        if (mode.specialMacros == SpecialMacroParsingMode.FORBID && macroName != null && SPECIAL_EXPR_MACROS.contains(macroName)) {
            return false;
        }

        // foo! bar {}
        //      ^ this ident
        boolean hasIdent = consumeTokenFast(b, IDENTIFIER);

        IElementType braceToken = b.getTokenType();
        MacroBraces braceKind = braceToken != null ? MacroBraces.fromToken(braceToken) : null;

        boolean trySpecialMacro = mode.specialMacros == SpecialMacroParsingMode.PARSE_AS_SPECIAL
            && macroName != null
            && !hasIdent;
        if (trySpecialMacro && braceKind != null) {
            SpecialMacroParser specialParser = SPECIAL_MACRO_PARSERS.get(macroName);
            if (specialParser != null && specialParser.parse(b, level + 1)) {
                if (braceKind.getNeedsSemicolon() && mode.semicolon && !consumeToken(b, SEMICOLON)) {
                    String tokenText = b.getTokenText();
                    b.error(RsBundle.message("parsing.error.expected.got2", tokenText != null ? tokenText : ""));
                    return mode.pin;
                }
                return true;
            }
        }

        if (braceKind == null || !parseMacroArgumentLazy(b, level + 1)) {
            String tokenText = b.getTokenText();
            b.error(RsBundle.message("parsing.error.macro.argument.expected.got", tokenText != null ? tokenText : ""));
            return mode.pin;
        }
        if (braceKind.getNeedsSemicolon() && mode.semicolon && !consumeToken(b, SEMICOLON)) {
            String tokenText = b.getTokenText();
            b.error(RsBundle.message("parsing.error.expected.got", tokenText != null ? tokenText : ""));
            return mode.pin;
        }
        return true;
    }

    @Nullable
    private static String getMacroName(@NotNull PsiBuilder b, int nameTokenIndex) {
        if (nameTokenIndex >= 0) {
            throw new IllegalArgumentException("`getMacroName` assumes path is already parsed and name token is behind current position");
        }

        int steps = 0;
        int meaningfulSteps = 0;

        while (meaningfulSteps > nameTokenIndex) {
            steps--;
            IElementType elementType = b.rawLookup(steps);
            if (elementType == null) return null;

            if (!isWhitespaceOrComment(b, elementType)) {
                meaningfulSteps--;
            }
        }

        if (b.rawLookup(steps) == IDENTIFIER) {
            return ParserUtil.rawLookupText(b, steps).toString();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static boolean gtgteqImpl(@NotNull PsiBuilder b, int level) { return collapse(b, GTGTEQ, GT, GT, EQ); }
    @SuppressWarnings("unused")
    public static boolean gtgtImpl(@NotNull PsiBuilder b, int level) { return collapse(b, GTGT, GT, GT); }
    @SuppressWarnings("unused")
    public static boolean gteqImpl(@NotNull PsiBuilder b, int level) { return collapse(b, GTEQ, GT, EQ); }
    @SuppressWarnings("unused")
    public static boolean ltlteqImpl(@NotNull PsiBuilder b, int level) { return collapse(b, LTLTEQ, LT, LT, EQ); }
    @SuppressWarnings("unused")
    public static boolean ltltImpl(@NotNull PsiBuilder b, int level) { return collapse(b, LTLT, LT, LT); }
    @SuppressWarnings("unused")
    public static boolean lteqImpl(@NotNull PsiBuilder b, int level) { return collapse(b, LTEQ, LT, EQ); }
    @SuppressWarnings("unused")
    public static boolean ororImpl(@NotNull PsiBuilder b, int level) { return collapse(b, OROR, OR, OR); }
    @SuppressWarnings("unused")
    public static boolean andandImpl(@NotNull PsiBuilder b, int level) { return collapse(b, ANDAND, AND, AND); }

    private static final TokenSet DEFAULT_NEXT_ELEMENTS = tokenSetOf(EXCL);
    private static final TokenSet ASYNC_NEXT_ELEMENTS = tokenSetOf(LBRACE, MOVE, OR);
    private static final TokenSet TRY_NEXT_ELEMENTS = tokenSetOf(LBRACE);

    @SuppressWarnings("unused")
    public static boolean defaultKeyword(@NotNull PsiBuilder b, int level) { return contextualKeyword(b, "default", DEFAULT, null); }
    @SuppressWarnings("unused")
    public static boolean unionKeyword(@NotNull PsiBuilder b, int level) { return contextualKeyword(b, "union", UNION, null); }
    @SuppressWarnings("unused")
    public static boolean autoKeyword(@NotNull PsiBuilder b, int level) { return contextualKeyword(b, "auto", AUTO, null); }
    @SuppressWarnings("unused")
    public static boolean dynKeyword(@NotNull PsiBuilder b, int level) { return contextualKeyword(b, "dyn", DYN, null); }
    @SuppressWarnings("unused")
    public static boolean asyncKeyword(@NotNull PsiBuilder b, int level) { return contextualKeyword(b, "async", ASYNC, null); }

    @SuppressWarnings("unused")
    public static boolean asyncBlockKeyword(@NotNull PsiBuilder b, int level) {
        return contextualKeyword(b, "async", ASYNC, it -> ASYNC_NEXT_ELEMENTS.contains(it));
    }

    @SuppressWarnings("unused")
    public static boolean tryKeyword(@NotNull PsiBuilder b, int level) {
        return contextualKeyword(b, "try", TRY, it -> TRY_NEXT_ELEMENTS.contains(it));
    }

    @SuppressWarnings("unused")
    public static boolean rawKeyword(@NotNull PsiBuilder b, int level) {
        return contextualKeywordWithRollback(b, "raw", RAW);
    }

    @SuppressWarnings("unused")
    public static boolean parseSecondPlusInIncrement(@NotNull PsiBuilder b, int level) {
        return noWhiteSpaceBefore(b, PLUS);
    }

    @SuppressWarnings("unused")
    public static boolean parseSecondMinusInDecrement(@NotNull PsiBuilder b, int level) {
        return noWhiteSpaceBefore(b, MINUS);
    }

    private static boolean noWhiteSpaceBefore(@NotNull PsiBuilder b, @NotNull IElementType token) {
        if (b.getTokenType() == token && b.rawLookup(-1) == token) {
            b.advanceLexer();
            return true;
        }
        return false;
    }

    private static boolean collapse(@NotNull PsiBuilder b, @NotNull IElementType tokenType, @NotNull IElementType... parts) {
        for (int i = 0; i < parts.length; i++) {
            if (b.rawLookup(i) != parts[i]) return false;
        }
        PsiBuilder.Marker marker = b.mark();
        PsiBuilderUtil.advance(b, parts.length);
        marker.collapse(tokenType);
        return true;
    }

    /**
     * Collapses contextual tokens (like &&) to a single token.
     *
     * @return a 2-element array [IElementType, size] or null
     */
    @Nullable
    public static Object[] collapsedTokenType(@NotNull PsiBuilder b) {
        IElementType tokenType = b.getTokenType();
        if (tokenType == GT) {
            IElementType next1 = b.rawLookup(1);
            if (next1 == GT) {
                IElementType next2 = b.rawLookup(2);
                if (next2 == EQ) return new Object[]{GTGTEQ, 3};
                return new Object[]{GTGT, 2};
            }
            if (next1 == EQ) return new Object[]{GTEQ, 2};
        } else if (tokenType == LT) {
            IElementType next1 = b.rawLookup(1);
            if (next1 == LT) {
                IElementType next2 = b.rawLookup(2);
                if (next2 == EQ) return new Object[]{LTLTEQ, 3};
                return new Object[]{LTLT, 2};
            }
            if (next1 == EQ) return new Object[]{LTEQ, 2};
        } else if (tokenType == OR) {
            if (b.rawLookup(1) == OR) return new Object[]{OROR, 2};
        } else if (tokenType == AND) {
            if (b.rawLookup(1) == AND) return new Object[]{ANDAND, 2};
        } else if (tokenType == INTEGER_LITERAL) {
            if (b.rawLookup(1) == DOT) {
                IElementType next2 = b.rawLookup(2);
                if (next2 == INTEGER_LITERAL || next2 == FLOAT_LITERAL) return new Object[]{FLOAT_LITERAL, 3};
                if (next2 == IDENTIFIER) return null;
                return new Object[]{FLOAT_LITERAL, 2};
            }
        }
        return null;
    }

    private static boolean isBracedMacro(@NotNull LighterASTNode node, @NotNull PsiBuilder b) {
        if (node.getTokenType() != MACRO_EXPR) return false;
        int offset = getBuilderOffset(b);
        CharSequence text = b.getOriginalText().subSequence(node.getStartOffset() - offset, node.getEndOffset() - offset);
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '}' || c == ']' || c == ')') {
                return c == '}';
            }
        }
        return false;
    }

    /**
     * Non-zero if PsiBuilder is created with LighterLazyParseableNode chameleon.
     */
    private static int getBuilderOffset(@NotNull PsiBuilder b) {
        try {
            // Access internal `productions` field via cast
            java.lang.reflect.Field productionsField = null;
            Class<?> clazz = b.getClass();
            while (clazz != null) {
                try {
                    productionsField = clazz.getDeclaredField("productions");
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            if (productionsField != null) {
                productionsField.setAccessible(true);
                Object productions = productionsField.get(b);
                if (productions instanceof List) {
                    List<?> list = (List<?>) productions;
                    if (!list.isEmpty()) {
                        Object firstMarker = list.get(0);
                        java.lang.reflect.Method getStartOffset = firstMarker.getClass().getMethod("getStartOffset");
                        return (int) getStartOffset.invoke(firstMarker);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static boolean contextualKeyword(
        @NotNull PsiBuilder b,
        @NotNull String keyword,
        @NotNull IElementType elementType,
        @Nullable java.util.function.Predicate<IElementType> nextElementPredicate
    ) {
        if (nextElementPredicate == null) {
            nextElementPredicate = it -> !DEFAULT_NEXT_ELEMENTS.contains(it);
        }
        // Tricky: the token can be already remapped by some previous rule that was backtracked
        if (b.getTokenType() == elementType
            || (b.getTokenType() == IDENTIFIER && keyword.equals(b.getTokenText()) && nextElementPredicate.test(b.lookAhead(1)))) {
            b.remapCurrentToken(elementType);
            b.advanceLexer();
            return true;
        }
        return false;
    }

    private static boolean contextualKeywordWithRollback(@NotNull PsiBuilder b, @NotNull String keyword, @NotNull IElementType elementType) {
        if (b.getTokenType() == IDENTIFIER && keyword.equals(b.getTokenText())) {
            PsiBuilder.Marker marker = b.mark();
            b.advanceLexer();
            marker.collapse(elementType);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    public static boolean parseMacroArgumentLazy(@NotNull PsiBuilder builder, int level) {
        return parseTokenTreeLazy(builder, level, MACRO_ARGUMENT);
    }

    @SuppressWarnings("unused")
    public static boolean parseMacroBodyLazy(@NotNull PsiBuilder builder, int level) {
        return parseTokenTreeLazy(builder, level, MACRO_BODY);
    }

    @SuppressWarnings("unused")
    public static boolean parseTokenTreeLazy(@NotNull PsiBuilder builder, int level, @NotNull IElementType tokenTypeToCollapse) {
        IElementType firstToken = builder.getTokenType();
        if (firstToken == null || !LEFT_BRACES.contains(firstToken)) return false;
        IElementType rightBrace = MacroBraces.fromTokenOrFail(firstToken).getCloseToken();
        PsiBuilderUtil.parseBlockLazy(builder, firstToken, rightBrace, tokenTypeToCollapse);
        return true;
    }

    public static boolean hasProperTokenTreeBraceBalance(@NotNull CharSequence text, @NotNull Lexer lexer) {
        lexer.start(text);

        IElementType firstToken = lexer.getTokenType();
        if (firstToken == null || !LEFT_BRACES.contains(firstToken)) return false;
        IElementType rightBrace = MacroBraces.fromTokenOrFail(firstToken).getCloseToken();

        return PsiBuilderUtil.hasProperBraceBalance(text, lexer, firstToken, rightBrace);
    }

    @SuppressWarnings("unused")
    public static boolean parseAnyBraces(@NotNull PsiBuilder b, int level, @NotNull Parser param) {
        IElementType firstToken = b.getTokenType();
        if (firstToken == null || !LEFT_BRACES.contains(firstToken)) return false;
        MacroBraces leftBrace = MacroBraces.fromTokenOrFail(firstToken);
        PsiBuilder.Marker pos = b.mark();
        b.advanceLexer(); // Consume '{' or '(' or '['

        // Save root brace if not set
        int oldFlags = getFlags(b);
        MacroBraces oldRootBrace = getMacroBraces(oldFlags);
        if (oldRootBrace == null) {
            setFlags(b, setMacroBraces(oldFlags, leftBrace));
        }
        MacroBraces rootBrace = oldRootBrace != null ? oldRootBrace : leftBrace;

        try {
            if (!param.parse(b, level + 1)) {
                pos.rollbackTo();
                setFlags(b, oldFlags);
                return false;
            }

            IElementType lastToken = b.getTokenType();
            if (lastToken == null || !RIGHT_BRACES.contains(lastToken)) {
                b.error(RsBundle.message("parsing.error.expected2", leftBrace.getCloseText()));
                boolean closeResult = lastToken == null;
                if (closeResult) {
                    pos.drop();
                } else {
                    pos.rollbackTo();
                }
                setFlags(b, oldFlags);
                return closeResult;
            }

            MacroBraces rightBrace = MacroBraces.fromToken(lastToken);
            if (rightBrace == leftBrace) {
                b.advanceLexer(); // Consume '}' or ')' or ']'
            } else {
                b.error(RsBundle.message("parsing.error.expected", leftBrace.getCloseText()));
                if (leftBrace == rootBrace) {
                    // Recovery loop. Consume everything until rightBrace is leftBrace
                    while (rightBrace != leftBrace && !b.eof()) {
                        b.advanceLexer();
                        IElementType tokenType = b.getTokenType();
                        if (tokenType == null) break;
                        rightBrace = MacroBraces.fromToken(tokenType);
                    }
                    b.advanceLexer();
                }
            }

            pos.drop();
            return true;
        } finally {
            setFlags(b, oldFlags);
        }
    }

    @SuppressWarnings("unused")
    public static boolean parseCodeBlockLazy(@NotNull PsiBuilder builder, int level) {
        return PsiBuilderUtil.parseBlockLazy(builder, LBRACE, RBRACE, BLOCK) != null;
    }

    @SuppressWarnings("unused")
    public static boolean parseSimplePat(@NotNull PsiBuilder builder) {
        return RustParser.SimplePat(builder, 0);
    }

    @Nullable
    private static Frame ancestorOfTypeOrSelf(@Nullable Frame frame, @NotNull IElementType elementType) {
        if (frame == null) return null;
        if (frame.elementType == elementType) return frame;
        return ancestorOfTypeOrSelf(frame.parentFrame, elementType);
    }
}
