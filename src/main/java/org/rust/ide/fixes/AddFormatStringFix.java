/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsPsiFactory;

import java.util.List;
import java.util.stream.Collectors;

public class AddFormatStringFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final int formatStringPosition;

    public AddFormatStringFix(@NotNull RsMacroCall call, int formatStringPosition) {
        super(call);
        this.formatStringPosition = formatStringPosition;
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.format.string");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @Nullable Editor editor,
                       @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        if (!(startElement instanceof RsMacroCall)) return;
        RsMacroCall call = (RsMacroCall) startElement;
        var formatMacroArgument = call.getFormatMacroArgument();
        if (formatMacroArgument == null) return;
        var arguments = formatMacroArgument.getFormatMacroArgList();
        var existingArgument = formatStringPosition < arguments.size() ? arguments.get(formatStringPosition) : null;
        PsiElement anchor = existingArgument;
        if (anchor == null) {
            anchor = formatMacroArgument.getRbrace();
            if (anchor == null) anchor = formatMacroArgument.getRbrack();
            if (anchor == null) anchor = formatMacroArgument.getRparen();
        }
        if (anchor == null) return;

        RsPsiFactory factory = new RsPsiFactory(project);
        String formatString = arguments.subList(formatStringPosition, arguments.size()).stream()
            .map(a -> "{}")
            .collect(Collectors.joining(" ", "\"", "\""));
        var formatStringArgument = factory.createFormatMacroArg(formatString);
        if (formatStringPosition != 0 && existingArgument == null) {
            formatMacroArgument.addBefore(factory.createComma(), anchor);
        }
        formatMacroArgument.addBefore(formatStringArgument, anchor);
        if (existingArgument != null) {
            formatMacroArgument.addBefore(factory.createComma(), anchor);
        }
    }
}
