/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.surroundWith.statement;

import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.language.editor.surroundWith.SurroundDescriptor;
import consulo.language.editor.surroundWith.Surrounder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.rust.ide.utils.RsBackendUtilUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import org.rust.lang.RsLanguage;

@ExtensionImpl
public class RsStatementsSurroundDescriptor implements SurroundDescriptor {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


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
