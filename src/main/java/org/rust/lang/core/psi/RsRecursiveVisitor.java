/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveVisitor;
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
