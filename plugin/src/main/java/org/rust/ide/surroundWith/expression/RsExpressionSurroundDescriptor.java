/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.expression;

import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.editor.surroundWith.SurroundDescriptor;
import consulo.language.editor.surroundWith.Surrounder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.rust.ide.utils.RsBackendUtilUtil;

public class RsExpressionSurroundDescriptor implements SurroundDescriptor {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    private static final Surrounder[] SURROUNDERS = new Surrounder[]{
        new RsWithParenthesesSurrounder(),
        new RsWithNotSurrounder(),
        new RsWithIfExpSurrounder(),
        new RsWithWhileExpSurrounder()
    };

    @Override
    public PsiElement[] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
        PsiElement expr = RsBackendUtilUtil.findExpressionInRange(file, startOffset, endOffset);
        if (expr == null) return PsiElement.EMPTY_ARRAY;
        FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.surroundwith.expression");
        return new PsiElement[]{expr};
    }

    @Override
    public Surrounder[] getSurrounders() {
        return SURROUNDERS;
    }

    @Override
    public boolean isExclusive() {
        return false;
    }
}
