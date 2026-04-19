/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.processors;

import com.intellij.application.options.CodeStyle;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.formatter.settings.RsCodeStyleSettings;

public final class RsFormatProcessorUtil {

    private RsFormatProcessorUtil() {
    }

    public static boolean shouldRunPunctuationProcessor(@NotNull ASTNode element) {
        if (!element.getPsi().isValid()) return false; // EA-110296, element might be invalid for some plugins
        PsiFile containingFile = element.getPsi().getContainingFile();
        return !CodeStyle.getCustomSettings(containingFile, RsCodeStyleSettings.class).PRESERVE_PUNCTUATION;
    }
}
