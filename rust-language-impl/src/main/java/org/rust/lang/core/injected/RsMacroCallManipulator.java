/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.injected;

import consulo.document.util.TextRange;
import consulo.language.psi.AbstractElementManipulator;
import consulo.util.lang.CharArrayUtil;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.impl.RsMacroCallUtil;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsMacroCallManipulator extends AbstractElementManipulator<RsMacroCall> {

    @Override
    @Nonnull
    public Class<RsMacroCall> getElementClass() {
        return RsMacroCall.class;
    }

    @Override
    public RsMacroCall handleContentChange(@Nonnull RsMacroCall element, @Nonnull TextRange range, @Nonnull String newContent) {
        String oldText = element.getText();
        String newText = oldText.substring(0, range.getStartOffset()) + newContent + oldText.substring(range.getEndOffset());

        RsMacroCall newMacroCall = (RsMacroCall) new RsPsiFactory(element.getProject()).createFile("m!" + newText).getFirstChild();
        if (newMacroCall == null) {
            throw new IllegalStateException(newText);
        }
        return (RsMacroCall) element.replace(newMacroCall);
    }

    @Nonnull
    @Override
    public TextRange getRangeInElement(@Nonnull RsMacroCall element) {
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
