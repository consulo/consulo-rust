/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.annotator.AnnotatorBase;
import org.rust.ide.fixes.AddFormatStringFix;
import org.rust.ide.injected.DoctestInfoUtil;
import org.rust.lang.core.macros.MacroExpansionManagerUtil;
import org.rust.lang.core.macros.MacroExpansionMode;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.utils.RsDiagnostic;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.List;
import org.rust.ide.injected.RsDoctestLanguageInjector;
import org.rust.lang.core.psi.ext.RsPossibleMacroCallUtil;

public class RsFormatMacroAnnotator extends AnnotatorBase {
    @Override
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof RsMacroCall)) return;
        RsMacroCall formatMacro = (RsMacroCall) element;
        if (!RsPossibleMacroCallUtil.getExistsAfterExpansion(formatMacro)) return;

        PsiElement resolved = formatMacro.getPath().getReference() != null ? formatMacro.getPath().getReference().resolve() : null;
        if (!(resolved instanceof RsMacro)) return;
        RsMacro resolvedMacro = (RsMacro) resolved;
        int[] formatMacroCtx = FormatImpl.getFormatMacroCtxPositionOnly(formatMacro, resolvedMacro);
        if (formatMacroCtx == null) return;
        int macroPos = formatMacroCtx[0];
        RsFormatMacroArgument formatMacroArgument = formatMacro.getFormatMacroArgument();
        if (formatMacroArgument == null) return;
        List<RsFormatMacroArg> macroArgs = formatMacroArgument.getFormatMacroArgList();

        RsFormatMacroArg formatStrArg = macroPos < macroArgs.size() ? macroArgs.get(macroPos) : null;
        RsLitExpr formatStr = formatStrArg != null && formatStrArg.getExpr() instanceof RsLitExpr
            ? (RsLitExpr) formatStrArg.getExpr() : null;

        ParseContext parseCtx = formatStr != null ? FormatImpl.parseParameters(formatStr) : null;
        if (parseCtx == null) {
            annotateMissingFormatString(formatMacro, resolvedMacro, macroArgs, macroPos, holder);
            return;
        }

        List<ErrorAnnotation> errors = FormatImpl.checkSyntaxErrors(parseCtx);
        for (ErrorAnnotation error : errors) {
            addAnnotation(holder, error, formatMacro);
        }

        if (!holder.isBatchMode()) {
            FormatImpl.highlightParametersOutside(parseCtx, holder);
        }

        if (!errors.isEmpty()) return;

        if (!holder.isBatchMode()) {
            FormatImpl.highlightParametersInside(parseCtx, holder);
        }

        boolean suppressTraitErrors = !OpenApiUtil.isUnitTestMode()
            && (!(MacroExpansionManagerUtil.getMacroExpansionManager(element.getProject()).getMacroExpansionMode() instanceof MacroExpansionMode.New)
            || RsDoctestLanguageInjector.isDoctestInjection(element));

        List<FormatParameter> parameters = FormatImpl.buildParameters(parseCtx);
        List<RsFormatMacroArg> arguments = new ArrayList<>();
        for (int i = macroPos + 1; i < macroArgs.size(); i++) {
            arguments.add(macroArgs.get(i));
        }
        FormatContext ctx = new FormatContext(parameters, arguments, formatMacro);

        List<ErrorAnnotation> annotations = new ArrayList<>(FormatImpl.checkParameters(ctx));
        annotations.addAll(FormatImpl.checkArguments(ctx));

        for (ErrorAnnotation annotation : annotations) {
            if (suppressTraitErrors && annotation.isTraitError()) continue;
            addAnnotation(holder, annotation, formatMacro);
        }
    }

    private void addAnnotation(@NotNull AnnotationHolder holder, @NotNull ErrorAnnotation error, @NotNull RsMacroCall call) {
        if (error.getDiagnostic() != null) {
            RsDiagnostic.addToHolder(error.getDiagnostic(), holder);
        } else {
            holder.newAnnotation(HighlightSeverity.ERROR, error.getError())
                .range(error.getRange())
                .create();
        }
    }

    private void annotateMissingFormatString(
        @NotNull RsMacroCall call,
        @NotNull RsMacro macro,
        @NotNull List<RsFormatMacroArg> macroArguments,
        int formatStringPosition,
        @NotNull AnnotationHolder holder
    ) {
        String macroName = macro.getName();
        if (macroName == null) return;
        if ("panic".equals(macroName)) return;
        RsFormatMacroArg formatString = formatStringPosition < macroArguments.size() ? macroArguments.get(formatStringPosition) : null;
        String message;
        com.intellij.openapi.util.TextRange range;
        if (formatString != null) {
            if (formatString.getExpr() instanceof RsMacroExpr) return;
            message = "Format argument must be a string literal";
            range = formatString.getTextRange();
        } else {
            if ("println".equals(macroName) || "eprintln".equals(macroName) || "writeln".equals(macroName)) return;
            message = "Requires at least a format string argument";
            range = call.getTextRange();
        }
        AddFormatStringFix fix = new AddFormatStringFix(call, formatStringPosition);
        holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(range)
            .withFix(fix)
            .create();
    }
}
