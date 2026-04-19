/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.injected;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;

public class RsMacroCallManipulator extends AbstractElementManipulator<RsMacroCall> {

    @Override
    public RsMacroCall handleContentChange(@NotNull RsMacroCall element, @NotNull TextRange range, @NotNull String newContent) {
        String oldText = element.getText();
        String newText = oldText.substring(0, range.getStartOffset()) + newContent + oldText.substring(range.getEndOffset());

        RsMacroCall newMacroCall = (RsMacroCall) new RsPsiFactory(element.getProject()).createFile("m!" + newText).getFirstChild();
        if (newMacroCall == null) {
            throw new IllegalStateException(newText);
        }
        return (RsMacroCall) element.replace(newMacroCall);
    }

    @NotNull
    @Override
    public TextRange getRangeInElement(@NotNull RsMacroCall element) {
        TextRange bodyTextRange = RsMacroCallUtil.getBodyTextRange(element);
        if (bodyTextRange == null) return super.getRangeInElement(element);
        TextRange shifted = bodyTextRange.shiftLeft(element.getTextRange().getStartOffset());
        String macroBody = RsMacroCallUtil.getMacroBody(element);
        if (macroBody == null) return shifted;

        int trimmedStart = shifted.getStartOffset();
        int trimmedEnd = shifted.getEndOffset();

        int firstNonSpaceIndex = CharArrayUtil.shiftForward(macroBody, 0, " \t");
        if (firstNonSpaceIndex < macroBody.length() && macroBody.charAt(firstNonSpaceIndex) == '\n') {
            trimmedStart = shifted.getStartOffset() + firstNonSpaceIndex + 1;
        }

        int lastNonSpaceIndex = CharArrayUtil.shiftBackward(macroBody, firstNonSpaceIndex, macroBody.length() - 1, " \t");
        if (lastNonSpaceIndex > firstNonSpaceIndex && macroBody.charAt(lastNonSpaceIndex) == '\n') {
            trimmedEnd = shifted.getEndOffset() - (macroBody.length() - lastNonSpaceIndex) + 1;
        }

        if (trimmedStart < trimmedEnd) {
            return new TextRange(trimmedStart, trimmedEnd);
        } else {
            return shifted;
        }
    }
}
