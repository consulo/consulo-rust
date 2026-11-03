/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.ide.injected;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.markdown.documentation.MarkdownDocumentationCodeFenceInjector;
import org.rust.lang.doc.psi.impl.RsDocCodeFenceImpl;

/**
 * Injects the language of the code fences of Rust doc comments naming another language than Rust, see
 * {@link RsDocMarkdownDocumentationProvider}.
 */
@ExtensionImpl
public class RsDocCodeFenceInjector extends MarkdownDocumentationCodeFenceInjector {
    @Override
    public Class<? extends PsiLanguageInjectionHost> getElementClass() {
        return RsDocCodeFenceImpl.class;
    }
}
