/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.injected;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.inject.advanced.AbstractLanguageInjectionSupport;
import consulo.language.inject.advanced.BaseInjection;
import consulo.language.inject.advanced.InjectorUtils;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Makes Rust a first-class host for language injection, so an injected language can be chosen for a
 * string literal or a macro call body and a preceding {@code language=...} comment is honoured.
 */
@ExtensionImpl
public class RsLanguageInjectionSupport extends AbstractLanguageInjectionSupport {

    public static final String ID = "rust";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Class[] getPatternClasses() {
        // Supplies the static pattern factories a place expression may call, e.g. rsLiteral().
        return new Class[]{RsPsiPattern.class};
    }

    @Override
    public boolean useDefaultInjector(PsiLanguageInjectionHost host) {
        // Lets injections configured against those patterns apply to Rust hosts.
        return true;
    }

    @Override
    public boolean isApplicableTo(PsiLanguageInjectionHost host) {
        return host instanceof RsElement;
    }

    /**
     * Reads an injection out of the nearest preceding comment, as in
     * <pre>
     * // language=RegExp
     * const PATTERN: &str = "abc(def)";
     * </pre>
     * The comment markers are not part of the comment value in Rust, so they are stripped before
     * the {@code language=...} declaration is parsed.
     */
    @Nullable
    @Override
    public BaseInjection findCommentInjection(@Nonnull PsiElement host, @Nullable Ref<PsiElement> commentRef) {
        return InjectorUtils.findNearestComment(host, comment -> {
            if (commentRef != null) {
                commentRef.set(comment);
            }
            return InjectorUtils.detectInjectionFromText(ID, commentText(comment));
        });
    }

    @Nonnull
    private static String commentText(@Nonnull PsiComment comment) {
        String text = comment.getText();
        if (text.startsWith("/*")) {
            // Covers /* */, /** */ and /*! */.
            int start = Math.min(prefixLength(text, "/*"), text.length());
            int end = text.endsWith("*/") ? text.length() - 2 : text.length();
            return start <= end ? text.substring(start, end).trim() : "";
        }
        if (text.startsWith("//")) {
            // Covers //, /// and //!.
            return text.substring(Math.min(prefixLength(text, "//"), text.length())).trim();
        }
        return text.trim();
    }

    private static int prefixLength(@Nonnull String text, @Nonnull String marker) {
        int length = marker.length();
        if (text.length() > length) {
            char next = text.charAt(length);
            if (next == '*' || next == '!' || next == '/') {
                return length + 1;
            }
        }
        return length;
    }
}
