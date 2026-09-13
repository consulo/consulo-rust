/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.builtin;

import consulo.language.parser.PsiBuilder;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiUtilCore;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.format.FormatImpl;
import org.rust.lang.core.format.FormatParameter;
import org.rust.lang.core.format.ParameterLookup;
import org.rust.lang.core.lexer.LexerUtilUtil;
import org.rust.lang.core.macros.*;
import org.rust.lang.core.macros.errors.BuiltinMacroExpansionError;
import org.rust.lang.core.parser.RustParser;
import org.rust.lang.core.parser.ParserUtil;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFormatMacroArgument;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import org.rust.stdext.RsResult;

import java.util.*;
import org.rust.lang.core.psi.impl.RsTokenSets;
import org.rust.lang.core.format.ParseContext;
import org.rust.lang.core.macros.RangeMap;
import org.rust.lang.core.psi.RsFormatMacroArg;

/**
 * A macro expander for built-in macros like {@code concat!()} and {@code stringify!()}
 */
public class BuiltinMacroExpander extends MacroExpander<RsBuiltinMacroData, BuiltinMacroExpansionError> {
    private final Project myProject;

    private static final Set<String> BUILTIN_FORMAT_MACROS = new HashSet<>(Arrays.asList("format_args", "format_args_nl"));

    public static final int EXPANDER_VERSION = 3;

    public BuiltinMacroExpander(@Nonnull Project project) {
        myProject = project;
    }

    @Nonnull
    public Project getProject() {
        return myProject;
    }

    @Nonnull
    @Override
    public RsResult<Pair<CharSequence, RangeMap>, BuiltinMacroExpansionError> expandMacroAsTextWithErr(
        @Nonnull RsBuiltinMacroData def,
        @Nonnull RsMacroCallData call
    ) {
        MacroCallBody macroBody = call.getMacroBody();
        if (BUILTIN_FORMAT_MACROS.contains(def.getName()) && macroBody instanceof MacroCallBody.FunctionLike) {
            MacroCallBody.FunctionLike functionLike = (MacroCallBody.FunctionLike) macroBody;
            RsFormatMacroArgument formatMacro = parseMacroBodyAsFormat(myProject, functionLike);
            if (formatMacro == null) {
                return new RsResult.Err<>(BuiltinMacroExpansionError.INSTANCE);
            }
            MappedText result = handleFormatMacro(def.getName(), functionLike.getText(), formatMacro);
            if (result == null) {
                return new RsResult.Err<>(BuiltinMacroExpansionError.INSTANCE);
            }
            return new RsResult.Ok<Pair<CharSequence, org.rust.lang.core.macros.RangeMap>, BuiltinMacroExpansionError>(new Pair<>(result.getText(), result.getRanges()));
        }
        return new RsResult.Err<>(BuiltinMacroExpansionError.INSTANCE);
    }

    @Nullable
    private static RsFormatMacroArgument parseMacroBodyAsFormat(@Nonnull Project project, @Nonnull MacroCallBody.FunctionLike macroBody) {
        String text = "(" + macroBody.getText() + ")";

        PsiBuilder adaptedBuilder = ParserUtil.createAdaptedRustPsiBuilder(project, text);
        Pair<PsiBuilder, RangeMap> lowered = DocsLowering.lowerDocCommentsToAdaptedPsiBuilder(adaptedBuilder, project);
        PsiBuilder builder = lowered.getFirst();

        boolean result = RustParser.FormatMacroArgument(builder, 0);
        if (!result) return null;

        PsiElement psi = builder.getTreeBuilt().getPsi();
        if (psi instanceof RsFormatMacroArgument) {
            return (RsFormatMacroArgument) psi;
        }
        return null;
    }

    @Nullable
    private static MappedText handleFormatMacro(@Nonnull String macroName, @Nonnull String macroText, @Nonnull RsFormatMacroArgument format) {
        List<? extends PsiElement> argList = format.getFormatMacroArgList();
        if (argList.isEmpty()) return null;

        PsiElement firstArg = argList.get(0);
        PsiElement expr = null;
        // Try to get the expr from the first format macro arg
        if (firstArg instanceof org.rust.lang.core.psi.RsFormatMacroArg) {
            expr = ((org.rust.lang.core.psi.RsFormatMacroArg) firstArg).getExpr();
        }
        if (!(expr instanceof RsLitExpr)) return null;
        RsLitExpr formatStr = (RsLitExpr) expr;

        org.rust.lang.core.format.ParseContext parseCtx = FormatImpl.parseParameters(formatStr);
        if (parseCtx == null) return null;
        List<?> errors = FormatImpl.checkSyntaxErrors(parseCtx);
        if (!errors.isEmpty()) return null;

        Set<String> namedArguments = new HashSet<>();
        for (PsiElement argument : argList) {
            if (argument instanceof org.rust.lang.core.psi.RsFormatMacroArg) {
                String name = FormatImpl.getArgName((org.rust.lang.core.psi.RsFormatMacroArg) argument);
                if (name != null) {
                    namedArguments.add(name);
                }
            }
        }

        MutableMappedText macroBuilder = new MutableMappedText(macroText.length() * 2);
        macroBuilder.appendUnmapped(macroName + "!(");
        macroBuilder.appendMapped(macroText, 0);

        PsiElement lastChild = format.getLastChild();
        PsiElement prevNonComment = lastChild != null ? PsiElementUtil.getPrevNonCommentSibling(lastChild) : null;
        boolean endsWithComma = prevNonComment != null && PsiUtilCore.getElementType(prevNonComment) == RsElementTypes.COMMA;

        boolean hasChanges = false;
        List<FormatParameter> parameters = FormatImpl.buildParameters(parseCtx);
        for (FormatParameter parameter : parameters) {
            if (parameter.getLookup() instanceof ParameterLookup.Named) {
                String name = ((ParameterLookup.Named) parameter.getLookup()).getName();
                IElementType tokenType = LexerUtilUtil.getRustLexerTokenType(name);
                if (!RsTokenSets.RS_IDENTIFIER_TOKENS.contains(tokenType)) {
                    return null;
                }
                if (!namedArguments.contains(name)) {
                    // Candidate for implicit argument
                    // We subtract 1 because we have added '(' to the beginning of the format so that it could be parsed
                    if (!endsWithComma) {
                        macroBuilder.appendUnmapped(", ");
                        endsWithComma = false;
                    } else {
                        String text = macroBuilder.getText().toString();
                        if (text.isEmpty() || text.charAt(text.length() - 1) != ' ') {
                            macroBuilder.appendUnmapped(" ");
                        }
                    }
                    macroBuilder.appendUnmapped(name + " = ");
                    macroBuilder.appendMapped(name, parameter.getRange().getStartOffset() - 1);
                    hasChanges = true;
                }
            }
        }

        if (!hasChanges) {
            return null;
        }

        macroBuilder.appendUnmapped(")");
        return macroBuilder.toMappedText();
    }
}
