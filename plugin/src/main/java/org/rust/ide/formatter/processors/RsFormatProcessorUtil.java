/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter.processors;

import consulo.language.codeStyle.CodeStyle;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import org.rust.ide.formatter.settings.RsCodeStyleSettings;

public final class RsFormatProcessorUtil {

    private RsFormatProcessorUtil() {
    }

    public static boolean shouldRunPunctuationProcessor(@Nonnull ASTNode element) {
        if (!element.getPsi().isValid()) return false; // EA-110296, element might be invalid for some plugins
        PsiFile containingFile = element.getPsi().getContainingFile();
        return !CodeStyle.getCustomSettings(containingFile, RsCodeStyleSettings.class).PRESERVE_PUNCTUATION;
    }
}
