/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ControlFlowElements {
    @Nullable
    private final PsiElement myControlFlowOwner;
    @NotNull
    private final List<PsiElement> myControlFlowElements;

    public ControlFlowElements(@Nullable PsiElement controlFlowOwner, @NotNull List<PsiElement> controlFlowElements) {
        myControlFlowOwner = controlFlowOwner;
        myControlFlowElements = controlFlowElements;
    }

    @Nullable
    public PsiElement getControlFlowOwner() {
        return myControlFlowOwner;
    }

    @NotNull
    public List<PsiElement> getControlFlowElements() {
        return myControlFlowElements;
    }
}
