/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsPsiFactory;

import java.util.List;
import java.util.stream.Collectors;
import consulo.localize.LocalizeValue;

public class AddFormatStringFix extends LocalQuickFixAndIntentionActionOnPsiElement {
    private final int formatStringPosition;

    public AddFormatStringFix(@Nonnull RsMacroCall call, int formatStringPosition) {
        super(call);
        this.formatStringPosition = formatStringPosition;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.format.string"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiFile file, @Nullable Editor editor,
                       @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
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
