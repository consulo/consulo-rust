/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.injected;

import consulo.language.inject.MultiHostInjector;
import consulo.language.inject.MultiHostRegistrar;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import org.intellij.lang.regexp.RegExpLanguage;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.RsLiteralKindUtil;

/**
 * Injects RegExpr language to a string literals in context like
 * {@code Regex::new("...")} and {@code RegexSet::new(&["...", "...", "..."])}
 */
public class RsRegExpInjector implements MultiHostInjector {

    @Override
    public Class<? extends PsiElement> getElementClass() {
        return RsLitExpr.class;
    }

    @Nonnull
    public List<Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(RsLitExpr.class);
    }

    @Override
    public void injectLanguages(@Nonnull MultiHostRegistrar registrar, @Nonnull PsiElement context) {
        if (!context.isValid() || !(context instanceof RsLitExpr)) return;
        RsLitExpr litExpr = (RsLitExpr) context;
        RsLiteralKind kind = RsLiteralKindUtil.getKind(litExpr);
        if (!(kind instanceof RsLiteralKind.StringLiteral)) return;
        TextRange range = ((RsLiteralKind.StringLiteral) kind).getOffsets().getValue();
        if (range == null) return;
        if (shouldInject(litExpr)) {
            registrar.startInjecting(RegExpLanguage.INSTANCE)
                .addPlace(null, null, litExpr, range)
                .doneInjecting();
        }
    }

    private static boolean shouldInject(@Nonnull RsLitExpr context) {
        return isRegexNew(context) || isRegexSetNew(context);
    }

    private static boolean isRegexNew(@Nonnull RsLitExpr context) {
        PsiElement parent1 = context.getParent();
        if (parent1 == null) return false;
        PsiElement parent2 = parent1.getParent();
        if (!(parent2 instanceof RsCallExpr)) return false;
        PsiElement expr = ((RsCallExpr) parent2).getExpr();
        if (!(expr instanceof RsPathExpr)) return false;
        return "Regex::new".equals(((RsPathExpr) expr).getPath().getText());
    }

    private static boolean isRegexSetNew(@Nonnull RsLitExpr context) {
        PsiElement p1 = context.getParent();
        if (p1 == null) return false;
        PsiElement p2 = p1.getParent();
        if (p2 == null) return false;
        PsiElement p3 = p2.getParent();
        if (p3 == null) return false;
        PsiElement p4 = p3.getParent();
        if (!(p4 instanceof RsCallExpr)) return false;
        PsiElement expr = ((RsCallExpr) p4).getExpr();
        if (!(expr instanceof RsPathExpr)) return false;
        return "RegexSet::new".equals(((RsPathExpr) expr).getPath().getText());
    }
}
