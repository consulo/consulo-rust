/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.ide.injected;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.markdown.documentation.MarkdownDocumentationProvider;
import org.jspecify.annotations.Nullable;
import org.rust.lang.core.injected.DoctestInfo;
import org.rust.lang.doc.psi.RsDocCodeFenceLang;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.doc.psi.impl.RsDocCodeFenceImpl;

import java.util.List;

/**
 * Rust doc comments as Markdown documentation: the code fences naming another language than Rust get that language injected.
 * Rust fences are doctests, injected by {@link org.rust.lang.core.injected.RsDoctestLanguageInjector}.
 */
@ExtensionImpl
public class RsDocMarkdownDocumentationProvider implements MarkdownDocumentationProvider {
    // "*" for the decoration of the lines of /** */ and /*! */ comments
    private static final List<String> LINE_PREFIXES = List.of("///", "//!", "*");

    @Override
    public Class<? extends PsiElement> getCommentClass() {
        return RsDocComment.class;
    }

    @Override
    public List<String> getLinePrefixes() {
        return LINE_PREFIXES;
    }

    @Override
    public Class<? extends PsiLanguageInjectionHost> getCodeFenceClass() {
        return RsDocCodeFenceImpl.class;
    }

    @Override
    public @Nullable String getCodeFenceInfo(PsiLanguageInjectionHost codeFence) {
        RsDocCodeFenceLang lang = ((RsDocCodeFenceImpl) codeFence).getLang();
        if (lang == null) {
            return null;
        }
        String info = lang.getText();
        return DoctestInfo.isRustLanguage(info) ? null : info;
    }
}
