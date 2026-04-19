/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.injected;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.intellij.lang.regexp.RegExpLanguage;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.RsLiteralKindUtil;

/**
 * Injects RegExpr language to a string literals in context like
 * {@code Regex::new("...")} and {@code RegexSet::new(&["...", "...", "..."])}
 */
public class RsRegExpInjector implements MultiHostInjector {

    @NotNull
    @Override
    public List<Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(RsLitExpr.class);
    }

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
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

    private static boolean shouldInject(@NotNull RsLitExpr context) {
        return isRegexNew(context) || isRegexSetNew(context);
    }

    private static boolean isRegexNew(@NotNull RsLitExpr context) {
        PsiElement parent1 = context.getParent();
        if (parent1 == null) return false;
        PsiElement parent2 = parent1.getParent();
        if (!(parent2 instanceof RsCallExpr)) return false;
        PsiElement expr = ((RsCallExpr) parent2).getExpr();
        if (!(expr instanceof RsPathExpr)) return false;
        return "Regex::new".equals(((RsPathExpr) expr).getPath().getText());
    }

    private static boolean isRegexSetNew(@NotNull RsLitExpr context) {
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
