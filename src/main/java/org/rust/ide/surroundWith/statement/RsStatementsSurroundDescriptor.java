/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.statement;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.rust.ide.utils.RsBackendUtilUtil;

public class RsStatementsSurroundDescriptor implements SurroundDescriptor {

    private static final Surrounder[] SURROUNDERS = new Surrounder[]{
        new RsWithBlockSurrounder(),
        new RsWithLoopSurrounder(),
        new RsWithWhileSurrounder(),
        new RsWithIfSurrounder(),
        new RsWithForSurrounder()
    };

    @Override
    public PsiElement[] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
        PsiElement[] stmts = RsBackendUtilUtil.findStatementsInRange(file, startOffset, endOffset);
        FeatureUsageTracker.getInstance().triggerFeatureUsed("codeassists.surroundwith.expression");
        return stmts;
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
