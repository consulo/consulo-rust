/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsBreakExpr;

import java.util.function.Consumer;

public final class RsLabeledExpressionUtil {
    private RsLabeledExpressionUtil() {
    }

    public static void processBreakExprs(@NotNull RsLabeledExpression expr,
                                          @Nullable String label,
                                          boolean matchOnlyByLabel,
                                          @NotNull Consumer<RsBreakExpr> sink) {
        processBreakExprsImpl(expr, label, matchOnlyByLabel, sink);
    }

    private static void processBreakExprsImpl(@NotNull PsiElement element,
                                               @Nullable String label,
                                               boolean matchOnlyByLabel,
                                               @NotNull Consumer<RsBreakExpr> sink) {
        for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof RsBreakExpr) {
                RsBreakExpr breakExpr = (RsBreakExpr) child;
                processBreakExprsImpl(breakExpr, label, matchOnlyByLabel, sink);
                boolean matches = (!matchOnlyByLabel && breakExpr.getLabel() == null)
                    || (breakExpr.getLabel() != null && label != null
                    && label.equals(breakExpr.getLabel().getReferenceName()));
                if (matches) {
                    sink.accept(breakExpr);
                }
            } else if (child instanceof RsLooplikeExpr) {
                if (label != null) {
                    processBreakExprsImpl(child, label, true, sink);
                }
            } else {
                processBreakExprsImpl(child, label, matchOnlyByLabel, sink);
            }
        }
    }
}
