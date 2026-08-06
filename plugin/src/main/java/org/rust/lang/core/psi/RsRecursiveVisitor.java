/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.application.progress.ProgressManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiRecursiveVisitor;
import org.rust.lang.core.psi.ext.RsElement;

public class RsRecursiveVisitor extends RsVisitor implements PsiRecursiveVisitor {
    @Override
    public void visitElement(PsiElement element) {
        ProgressManager.checkCanceled();
        element.acceptChildren(this);
    }

    @Override
    public void visitElement(RsElement element) {
        visitElement((PsiElement) element);
    }
}
