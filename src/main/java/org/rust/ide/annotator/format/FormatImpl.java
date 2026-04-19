/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.colors.RsColor;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.core.FeatureAvailability;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.utils.RsStringCharactersParsing;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.rust.lang.core.psi.RsLiteralKindUtil;

public final class FormatImpl {
    private static final Pattern FORMAT_PARSER = Pattern.compile("\\{\\{|}}|(\\{([^}]*)}?)|(})");
    private static final Pattern FORMAT_PARAMETER_PARSER = Pattern.compile(
        "(?x)" +
        "^(?<id>[a-zA-Z_][\\w+]*|\\d+)?" +
        "(:(.?[\\^<>])?[+\\-]?\\#?" +
        "0?(?!\\$)" +
        "(?<width>[a-zA-Z_][\\w+]*\\$|\\d+\\$|\\d+)?" +
        "(\\.(?<precision>[a-zA-Z_][\\w+]*\\$|\\d+\\$|\\d+|\\*)?)?" +
        "(?<type>\\w?\\??)?" +
        ")?\\s*"
    );

    private FormatImpl() {
    }

    @Nullable
    public static ParseContext parseParameters(@NotNull RsLitExpr formatStr) {
        RsLiteralKind kind = RsLiteralKindUtil.getKind(formatStr);
        if (!(kind instanceof RsLiteralKind.StringLiteral)) return null;
        RsLiteralKind.StringLiteral literalKind = (RsLiteralKind.StringLiteral) kind;

        if (RsTokenType.RS_BYTE_STRING_LITERALS.contains(literalKind.getNode().getElementType())) return null;
        if (RsTokenType.RS_CSTRING_LITERALS.contains(literalKind.getNode().getElementType())) return null;

        TextRange rawTextRange = literalKind.getOffsets().getValue();
        if (rawTextRange == null) return null;
        String text = literalKind.getRawValue();
        if (text == null) return null;

        String unescapedText;
        int[] sourceMap;
        if (literalKind.getNode().getElementType() == RsElementTypes.RAW_STRING_LITERAL) {
            sourceMap = new int[text.length()];
            for (int i = 0; i < text.length(); i++) sourceMap[i] = i;
            unescapedText = text;
        } else {
            RsStringCharactersParsing.Result result = RsStringCharactersParsing.parseRustStringCharacters(text);
            unescapedText = result.getParsedText().toString();
            sourceMap = result.getSourceMap();
        }

        Matcher matcher = FORMAT_PARSER.matcher(unescapedText);
        List<ParsedParameter> parsed = new ArrayList<>();
        while (matcher.find()) {
            java.util.regex.MatchResult matchResult = matcher.toMatchResult();
            if (matchResult.group(1) != null) {
                String innerContent = matchResult.group(2);
                if (innerContent == null) innerContent = "";
                Matcher innerMatcher = FORMAT_PARAMETER_PARSER.matcher(innerContent);
                java.util.regex.MatchResult innerResult = innerMatcher.find() ? innerMatcher.toMatchResult() : null;
                parsed.add(new ParsedParameter(matchResult, innerResult));
            } else {
                parsed.add(new ParsedParameter(matchResult));
            }
        }

        int offset = RsElementUtil.getStartOffset(formatStr) + rawTextRange.getStartOffset();
        return new ParseContext(sourceMap, offset, parsed);
    }

    @NotNull
    public static List<ErrorAnnotation> checkSyntaxErrors(@NotNull ParseContext ctx) {
        List<ErrorAnnotation> errors = new ArrayList<>();

        for (ParsedParameter parameter : ctx.getParameters()) {
            java.util.regex.MatchResult completeMatch = parameter.getCompleteMatch();
            TextRange range = ctx.toSourceRange(parameter.getRangeStart(), parameter.getRangeEnd());

            if (completeMatch.group(3) != null) {
                errors.add(new ErrorAnnotation(range, RsBundle.message("inspection.message.invalid.format.string.unmatched")));
            }

            // Additional syntax error checks would go here
        }
        return errors;
    }

    public static void highlightParametersOutside(@NotNull ParseContext ctx, @NotNull AnnotationHolder holder) {
        RsColor key = RsColor.FORMAT_PARAMETER;
        HighlightSeverity highlightSeverity = OpenApiUtil.isUnitTestMode() ? key.getTestSeverity() : HighlightSeverity.INFORMATION;

        for (ParsedParameter parameter : ctx.getParameters()) {
            TextRange range = ctx.toSourceRange(parameter.getRangeStart(), parameter.getRangeEnd());
            holder.newSilentAnnotation(highlightSeverity).range(range).textAttributes(key.getTextAttributesKey()).create();
        }
    }

    public static void highlightParametersInside(@NotNull ParseContext ctx, @NotNull AnnotationHolder holder) {
        // Simplified inner highlighting
        for (ParsedParameter parameter : ctx.getParameters()) {
            java.util.regex.MatchResult match = parameter.getInnerContentMatch();
            if (match == null) continue;
            // Inner parameter highlighting would go here
        }
    }

    @NotNull
    public static List<FormatParameter> buildParameters(@NotNull ParseContext ctx) {
        List<FormatParameter> result = new ArrayList<>();
        int implicitPositionCounter = 0;

        for (ParsedParameter param : ctx.getParameters()) {
            java.util.regex.MatchResult completeMatch = param.getCompleteMatch();
            java.util.regex.MatchResult match = param.getInnerContentMatch();
            if (match == null) continue;

            String idStr = getNamedGroup(match, "id");
            String typeStr = getNamedGroup(match, "type");
            if (typeStr == null) typeStr = "";

            TextRange paramRange = ctx.toSourceRange(param.getRangeStart(), param.getRangeEnd());
            TextRange typeRange = paramRange; // simplified

            if (idStr != null) {
                ParameterMatchInfo matchInfo = new ParameterMatchInfo(paramRange, completeMatch.group());
                result.add(new FormatParameter.Value(matchInfo, buildLookup(idStr), typeStr, typeRange));
            } else {
                ParameterMatchInfo matchInfo = new ParameterMatchInfo(paramRange, completeMatch.group());
                result.add(new FormatParameter.Value(matchInfo, new ParameterLookup.Positional(implicitPositionCounter++), typeStr, typeRange));
            }
        }

        return result;
    }

    @NotNull
    public static List<ErrorAnnotation> checkParameters(@NotNull FormatContext ctx) {
        boolean implicitNamedArgsAvailable = CompilerFeature.getFORMAT_ARGS_CAPTURE().availability(ctx.getMacro()) == FeatureAvailability.AVAILABLE;

        List<ErrorAnnotation> errors = new ArrayList<>();
        for (FormatParameter parameter : ctx.getParameters()) {
            ParameterLookup lookup = parameter.getLookup();
            if (lookup instanceof ParameterLookup.Named) {
                ParameterLookup.Named named = (ParameterLookup.Named) lookup;
                if (!ctx.getNamedArguments().containsKey(named.getName()) && !implicitNamedArgsAvailable) {
                    errors.add(new ErrorAnnotation(parameter.getRange(), RsBundle.message("inspection.message.there.no.argument.named", named.getName())));
                }
            } else if (lookup instanceof ParameterLookup.Positional) {
                ParameterLookup.Positional positional = (ParameterLookup.Positional) lookup;
                if (positional.getPosition() >= ctx.getArguments().size()) {
                    String count;
                    switch (ctx.getArguments().size()) {
                        case 0: count = RsBundle.message("inspection.message.no.arguments.were.given"); break;
                        case 1: count = RsBundle.message("inspection.message.there.argument"); break;
                        default: count = RsBundle.message("inspection.message.there.are.arguments", ctx.getArguments().size()); break;
                    }
                    errors.add(new ErrorAnnotation(parameter.getRange(), RsBundle.message("inspection.message.invalid.reference.to.positional.argument", positional.getPosition(), count)));
                }
            }

            if (errors.isEmpty() && parameter instanceof FormatParameter.Value) {
                FormatParameter.Value value = (FormatParameter.Value) parameter;
                if (value.getType() == null && !value.getTypeStr().isEmpty()) {
                    errors.add(new ErrorAnnotation(value.getTypeRange(), RsBundle.message("inspection.message.unknown.format.trait", value.getTypeStr())));
                }
            }
        }
        return errors;
    }

    @NotNull
    public static List<ErrorAnnotation> checkArguments(@NotNull FormatContext ctx) {
        List<ErrorAnnotation> errors = new ArrayList<>();
        for (int i = 0; i < ctx.getArguments().size(); i++) {
            RsFormatMacroArg arg = ctx.getArguments().get(i);
            String name = getArgName(arg);
            int position = i;
            boolean hasPositionalParameter = false;
            for (com.intellij.openapi.util.Pair<FormatParameter, ParameterLookup.Positional> pair : ctx.getPositionalParameters()) {
                if (pair.getSecond().getPosition() == position) {
                    hasPositionalParameter = true;
                    break;
                }
            }

            if (name == null) {
                int firstNamed = -1;
                for (int j = 0; j < ctx.getArguments().size(); j++) {
                    if (ctx.getArguments().get(j).getEq() != null) {
                        firstNamed = j;
                        break;
                    }
                }
                if (firstNamed != -1 && firstNamed < position) {
                    errors.add(new ErrorAnnotation(arg.getTextRange(), RsBundle.message("inspection.message.positional.arguments.cannot.follow.named.arguments")));
                } else if (!hasPositionalParameter) {
                    errors.add(new ErrorAnnotation(arg.getTextRange(), RsBundle.message("inspection.message.argument.never.used")));
                }
            } else {
                boolean hasNamedParameter = false;
                for (com.intellij.openapi.util.Pair<FormatParameter, ParameterLookup.Named> pair : ctx.getNamedParameters()) {
                    if (pair.getSecond().getName().equals(name)) {
                        hasNamedParameter = true;
                        break;
                    }
                }
                if (!hasNamedParameter && !hasPositionalParameter) {
                    errors.add(new ErrorAnnotation(arg.getTextRange(), RsBundle.message("inspection.message.named.argument.never.used")));
                }
            }
        }
        return errors;
    }

    @Nullable
    public static int[] getFormatMacroCtxPositionOnly(@NotNull RsMacroCall call, @NotNull RsMacro def) {
        String macroName = def.getName();
        if (macroName == null) return null;

        Object containingCrate = RsElementUtil.getContainingCrate(def);
        RsFormatMacroArgument formatMacroArgument = call.getFormatMacroArgument();
        if (containingCrate == null || formatMacroArgument == null) return null;

        List<RsFormatMacroArg> formatMacroArgs = formatMacroArgument.getFormatMacroArgList();
        PackageOrigin origin = RsElementUtil.getContainingCrateAsPackageOrigin(def);
        if (origin != PackageOrigin.STDLIB || formatMacroArgs == null) return null;

        Integer position;
        switch (macroName) {
            case "println":
            case "print":
            case "eprintln":
            case "eprint":
            case "format":
            case "format_args":
            case "format_args_nl":
                position = 0;
                break;
            case "panic":
                CargoWorkspace.Edition edition = RsElementUtil.getContainingCrateEdition(call);
                if (formatMacroArgs.size() < 2 && (edition == null || edition.compareTo(CargoWorkspace.Edition.EDITION_2021) < 0)) {
                    position = null;
                } else {
                    position = 0;
                }
                break;
            case "write":
            case "writeln":
                position = 1;
                break;
            default:
                position = null;
                break;
        }
        if (position == null) return null;
        return new int[] { position };
    }

    @Nullable
    public static String getArgName(@NotNull RsFormatMacroArg arg) {
        if (arg.getEq() == null) return null;
        com.intellij.lang.ASTNode identNode = arg.getNode().findChildByType(RsTokenType.RS_IDENTIFIER_TOKENS);
        return identNode != null ? identNode.getText() : null;
    }

    @NotNull
    private static ParameterLookup buildLookup(@NotNull String value) {
        try {
            int identifier = Integer.parseInt(value);
            return new ParameterLookup.Positional(identifier);
        } catch (NumberFormatException e) {
            return new ParameterLookup.Named(value);
        }
    }

    @Nullable
    private static String getNamedGroup(@NotNull java.util.regex.MatchResult match, @NotNull String name) {
        // Java regex MatchResult doesn't support named groups directly from MatchResult
        // This is a simplified implementation
        try {
            if (match instanceof Matcher) {
                return ((Matcher) match).group(name);
            }
        } catch (IllegalArgumentException e) {
            // Group name not found
        }
        return null;
    }
}
