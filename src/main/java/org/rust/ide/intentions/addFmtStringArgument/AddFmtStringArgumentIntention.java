/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.addFmtStringArgument;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.RsElementBaseIntentionAction;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.macros.MacroExpansionContextUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;

public class AddFmtStringArgumentIntention extends RsElementBaseIntentionAction<AddFmtStringArgumentIntention.Context> {

    @Override
    @NotNull
    public String getText() {
        return RsBundle.message("intention.name.add.format.string.argument");
    }

    @Override
    @NotNull
    public String getFamilyName() {
        return getText();
    }

    @Override
    @NotNull
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_EXPANSION;
    }

    @Override
    @NotNull
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_EXPANSION;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    @NotNull
    public PsiFile getElementToMakeWritable(@NotNull PsiFile currentFile) {
        return currentFile;
    }

    public static class Context {
        @NotNull
        public final RsLitExpr literal;
        @NotNull
        public final RsMacroCall macroCall;

        public Context(@NotNull RsLitExpr literal, @NotNull RsMacroCall macroCall) {
            this.literal = literal;
            this.macroCall = macroCall;
        }
    }

    @Override
    @Nullable
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        if (element.getNode().getElementType() != RsElementTypes.STRING_LITERAL) return null;
        RsLitExpr literal = RsElementUtil.ancestorOrSelf(element, RsLitExpr.class);
        if (literal == null) return null;

        // Caret must be inside a literal, not right before or right after it
        if (!RsElementUtil.containsOffset(literal, editor.getCaretModel().getOffset())) return null;

        RsMacroCall macroCall = RsElementUtil.ancestorOrSelf(literal, RsMacroCall.class);
        if (macroCall == null) return null;
        if (!MacroExpansionContextUtil.isExprOrStmtContext(macroCall) || !FORMAT_MACROS.contains(RsMacroCallUtil.getMacroName(macroCall)))
            return null;

        return new Context(literal, macroCall);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        RsElement macroCallExpr = (RsElement) ctx.macroCall.getParent();
        if (macroCallExpr == null) return;

        RsLitExpr literal = ctx.literal;
        if (!RsElementUtil.containsOffset(literal, editor.getCaretModel().getOffset())) return;

        int caretOffsetInLiteral = editor.getCaretModel().getOffset() - literal.getTextOffset() - 1;
        if (caretOffsetInLiteral < 0) return;

        String oldString = literal.getText().replace("\"", "");
        String oldStringUntilCaret = oldString.substring(0, caretOffsetInLiteral);
        Pattern placeholderRegex = Pattern.compile("\\{(:[a-zA-Z0-9.,?]*)?}");

        Matcher matcher = placeholderRegex.matcher(oldStringUntilCaret);
        int placeholderNumber = 0;
        while (matcher.find()) {
            placeholderNumber++;
        }

        RsExpressionCodeFragment codeFragment = new RsExpressionCodeFragment(project, CODE_FRAGMENT_TEXT, macroCallExpr);

        if (OpenApiUtil.isUnitTestMode()) {
            addFmtStringArgument(project, editor, ctx, codeFragment, caretOffsetInLiteral, placeholderNumber);
        } else {
            int finalPlaceholderNumber = placeholderNumber;
            RsAddFmtStringArgumentPopup.show(editor, project, codeFragment, () ->
                addFmtStringArgument(project, editor, ctx, codeFragment, caretOffsetInLiteral, finalPlaceholderNumber)
            );
        }
    }

    private void addFmtStringArgument(
        @NotNull Project project,
        @NotNull Editor editor,
        @NotNull Context ctx,
        @NotNull RsExpressionCodeFragment codeFragment,
        int caretOffsetInLiteral,
        int placeholderNumber
    ) {
        RsPsiFactory psiFactory = new RsPsiFactory(project);

        RsMacroCall macroCall = ctx.macroCall;
        RsFormatMacroArgument argument = macroCall.getFormatMacroArgument();
        if (argument == null) return;
        List<? extends RsFormatMacroArg> arguments = argument.getFormatMacroArgList();

        String newPlaceholder = "{}";
        String oldString = ctx.literal.getText().replace("\"", "");
        String prefix = oldString.substring(0, caretOffsetInLiteral);
        String suffix = oldString.substring(caretOffsetInLiteral);
        String newString = "\"" + prefix + newPlaceholder + suffix + "\"";
        RsExpr newArgExpr = codeFragment.getExpr();
        if (newArgExpr == null) return;
        String newArgument = newArgExpr.getText();

        List<String> newArgs;
        if (arguments.size() == 1) {
            newArgs = Arrays.asList(newString, newArgument);
        } else {
            int literalPosition = -1;
            for (int i = 0; i < arguments.size(); i++) {
                if (arguments.get(i).getExpr() == ctx.literal) {
                    literalPosition = i;
                    break;
                }
            }
            if (literalPosition < 0) return;

            List<String> argsBeforeLiteral = new ArrayList<>();
            for (int i = 0; i < literalPosition; i++) {
                argsBeforeLiteral.add(arguments.get(i).getText());
            }
            List<String> argsAfterLiteral = new ArrayList<>();
            for (int i = literalPosition + 1; i < arguments.size(); i++) {
                argsAfterLiteral.add(arguments.get(i).getText());
            }

            List<String> newArgsAfterLiteral = new ArrayList<>();
            newArgsAfterLiteral.addAll(argsAfterLiteral.subList(0, Math.min(placeholderNumber, argsAfterLiteral.size())));
            newArgsAfterLiteral.add(newArgument);
            if (placeholderNumber < argsAfterLiteral.size()) {
                newArgsAfterLiteral.addAll(argsAfterLiteral.subList(placeholderNumber, argsAfterLiteral.size()));
            }

            newArgs = new ArrayList<>();
            newArgs.addAll(argsBeforeLiteral);
            newArgs.add(newString);
            newArgs.addAll(newArgsAfterLiteral);
        }

        org.rust.lang.core.psi.MacroBraces bracesKind = RsMacroCallUtil.getBracesKind(macroCall);
        if (bracesKind == null) return;
        RsMacroCall newMacroCall = psiFactory.createMacroCall(
            org.rust.lang.core.macros.MacroExpansionContextUtil.getExpansionContext(macroCall),
            bracesKind,
            RsMacroCallUtil.getMacroName(macroCall),
            newArgs.toArray(new String[0])
        );

        String text = getText();
        project.getService(com.intellij.openapi.command.CommandProcessor.class);
        OpenApiUtil.runWriteCommandAction(project, text, () -> {
            IntentionInMacroUtil.finishActionInMacroExpansionCopy(editor);
            RsMacroCall inserted = (RsMacroCall) macroCall.replace(newMacroCall);
            org.rust.openapiext.EditorExt.moveCaretToOffset(editor, inserted, editor.getCaretModel().getOffset() + newPlaceholder.length());
        });
    }

    @Override
    @NotNull
    public IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    private static final Set<String> FORMAT_MACROS = new HashSet<>(Arrays.asList(
        "format", "write", "writeln", "print", "println", "eprint", "eprintln", "format_args"
    ));

    @VisibleForTesting
    @NotNull
    public static String CODE_FRAGMENT_TEXT = "";
}
