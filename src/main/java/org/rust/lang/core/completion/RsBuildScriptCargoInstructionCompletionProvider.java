/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Arrays;
import java.util.List;

import static org.rust.lang.core.PsiElementPatternExtUtil.or;
import org.rust.lang.core.psi.ext.RsLitExprUtil;

public class RsBuildScriptCargoInstructionCompletionProvider extends RsCompletionProvider {
    public static final RsBuildScriptCargoInstructionCompletionProvider INSTANCE = new RsBuildScriptCargoInstructionCompletionProvider();

    private static final Key<RsLiteralKind.StringLiteral> CARGO_INSTRUCTION_KEY = Key.create("CARGO_INSTRUCTION");
    private static final String CARGO_INSTRUCTION_PREFIX = "cargo:";

    private static final List<String> CARGO_INSTRUCTIONS = Arrays.asList(
        "rerun-if-changed",
        "rerun-if-env-changed",
        "rustc-link-arg",
        "rustc-link-arg-bin",
        "rustc-link-arg-bins",
        "rustc-link-arg-tests",
        "rustc-link-arg-examples",
        "rustc-link-arg-benches",
        "rustc-link-lib",
        "rustc-link-search",
        "rustc-flags",
        "rustc-cfg",
        "rustc-env",
        "rustc-cdylib-link-arg",
        "warning"
    );

    private RsBuildScriptCargoInstructionCompletionProvider() {
    }

    @NotNull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement()
            .andOr(
                PlatformPatterns.psiElement(RsElementTypes.STRING_LITERAL),
                PlatformPatterns.psiElement(RsElementTypes.RAW_STRING_LITERAL)
            )
            .with(new PatternCondition<PsiElement>("buildScriptCargoInstruction") {
                @Override
                public boolean accepts(@NotNull PsiElement e, @Nullable ProcessingContext ctx) {
                    PsiElement parent = e.getParent();
                    if (!(parent instanceof RsLitExpr)) return false;
                    RsLiteralKind kind = RsLiteralKindUtil.getKind((RsLitExpr) parent);
                    if (!(kind instanceof RsLiteralKind.StringLiteral)) return false;
                    PsiElement argParent = parent.getParent();
                    if (!(argParent instanceof RsFormatMacroArg)) return false;
                    PsiElement argumentsParent = argParent.getParent();
                    if (!(argumentsParent instanceof RsFormatMacroArgument)) return false;
                    List<RsFormatMacroArg> argList = ((RsFormatMacroArgument) argumentsParent).getFormatMacroArgList();
                    if (argList.isEmpty() || argList.get(0) != argParent) return false;
                    PsiElement macroCallElement = argumentsParent.getParent();
                    if (!(macroCallElement instanceof RsMacroCall)) return false;
                    String macroName = RsMacroCallUtil.getMacroName((RsMacroCall) macroCallElement);
                    if (!"print".equals(macroName) && !"println".equals(macroName)) return false;
                    if (!((RsMacroCall) macroCallElement).getContainingCrate().getKind().isCustomBuild()) return false;
                    if (ctx != null) {
                        ctx.put(CARGO_INSTRUCTION_KEY, (RsLiteralKind.StringLiteral) kind);
                    }
                    return true;
                }
            });
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        RsLiteralKind.StringLiteral kindString = context.get(CARGO_INSTRUCTION_KEY);
        if (kindString == null) return;
        LiteralOffsets offsets = kindString.getOffsets();
        TextRange value = offsets.getValue();
        if (value == null) return;

        PsiElement position = parameters.getPosition();
        String currentText = position.getText().substring(value.getStartOffset(), parameters.getOffset() - position.getTextRange().getStartOffset());

        if (currentText.startsWith(CARGO_INSTRUCTION_PREFIX)) {
            List<LookupElementBuilder> lookupElements = new java.util.ArrayList<>();
            for (String instruction : CARGO_INSTRUCTIONS) {
                lookupElements.add(
                    LookupElementBuilder.create(instruction)
                        .withInsertHandler((ctx, item) -> {
                            if (!LookupElements.nextCharIs(ctx, '=')) {
                                RsKeywordCompletionProvider.addSuffix(ctx, "=");
                            }
                        })
                );
            }
            result.withPrefixMatcher(currentText.substring(CARGO_INSTRUCTION_PREFIX.length()))
                .addAllElements(lookupElements);
        } else {
            result.addElement(
                LookupElementBuilder.create(CARGO_INSTRUCTION_PREFIX)
                    .withInsertHandler((ctx, item) ->
                        AutoPopupController.getInstance(ctx.getProject()).scheduleAutoPopup(ctx.getEditor()))
            );
        }
    }
}
