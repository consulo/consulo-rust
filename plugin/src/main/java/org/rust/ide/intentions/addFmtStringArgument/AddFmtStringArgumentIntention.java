/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.addFmtStringArgument;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import consulo.localize.LocalizeValue;
import consulo.undoRedo.CommandProcessor;
import org.rust.lang.core.psi.MacroBraces;
import org.rust.openapiext.EditorExt;

public class AddFmtStringArgumentIntention extends RsElementBaseIntentionAction<AddFmtStringArgumentIntention.Context> {

    @Override
    @Nonnull
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.format.string.argument"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Override
    @Nonnull
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_EXPANSION;
    }

    @Override
    @Nonnull
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_EXPANSION;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    @Nonnull
    public PsiFile getElementToMakeWritable(@Nonnull PsiFile currentFile) {
        return currentFile;
    }

    public static class Context {
        @Nonnull
        public final RsLitExpr literal;
        @Nonnull
        public final RsMacroCall macroCall;

        public Context(@Nonnull RsLitExpr literal, @Nonnull RsMacroCall macroCall) {
            this.literal = literal;
            this.macroCall = macroCall;
        }
    }

    @Override
    @Nullable
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
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
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
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
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull Context ctx,
        @Nonnull RsExpressionCodeFragment codeFragment,
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

        String text = getText().get();
        project.getService(consulo.undoRedo.CommandProcessor.class);
        OpenApiUtil.runWriteCommandAction(project, text, () -> {
            IntentionInMacroUtil.finishActionInMacroExpansionCopy(editor);
            RsMacroCall inserted = (RsMacroCall) macroCall.replace(newMacroCall);
            org.rust.openapiext.EditorExt.moveCaretToOffset(editor, inserted, editor.getCaretModel().getOffset() + newPlaceholder.length());
        });
    }

    @Nonnull
    public IntentionPreviewInfo generatePreview(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    private static final Set<String> FORMAT_MACROS = new HashSet<>(Arrays.asList(
        "format", "write", "writeln", "print", "println", "eprint", "eprintln", "format_args"
    ));

    
    @Nonnull
    public static String CODE_FRAGMENT_TEXT = "";
}
