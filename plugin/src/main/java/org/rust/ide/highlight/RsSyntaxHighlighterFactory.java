/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.highlight;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.highlight.SingleLazyInstanceSyntaxHighlighterFactory;
import consulo.language.editor.highlight.SyntaxHighlighter;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;

/**
 * Registers {@link RsHighlighter} for {@link RsLanguage}.
 */
@ExtensionImpl
public class RsSyntaxHighlighterFactory extends SingleLazyInstanceSyntaxHighlighterFactory {

    @Nonnull
    @Override
    protected SyntaxHighlighter createHighlighter() {
        return new RsHighlighter();
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return RsLanguage.INSTANCE;
    }
}
