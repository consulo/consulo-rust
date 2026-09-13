/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.highlight;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.rawHighlight.RainbowVisitor;
import consulo.language.editor.rawHighlight.RainbowVisitorFactory;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.impl.RsFile;

/**
 * Creates a fresh {@link RsRainbowVisitor} for every highlighting pass over a Rust file.
 */
@ExtensionImpl
public class RsRainbowVisitorFactory implements RainbowVisitorFactory {

    @Override
    public boolean suitableForFile(@Nonnull PsiFile file) {
        return file instanceof RsFile;
    }

    @Nonnull
    @Override
    public RainbowVisitor createVisitor() {
        return new RsRainbowVisitor();
    }
}
