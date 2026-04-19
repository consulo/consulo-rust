/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.expression;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.rust.ide.utils.RsBackendUtilUtil;

public class RsExpressionSurroundDescriptor implements SurroundDescriptor {

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
