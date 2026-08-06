/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public class ControlFlowElements {
    @Nullable
    private final PsiElement myControlFlowOwner;
    @Nonnull
    private final List<PsiElement> myControlFlowElements;

    public ControlFlowElements(@Nullable PsiElement controlFlowOwner, @Nonnull List<PsiElement> controlFlowElements) {
        myControlFlowOwner = controlFlowOwner;
        myControlFlowElements = controlFlowElements;
    }

    @Nullable
    public PsiElement getControlFlowOwner() {
        return myControlFlowOwner;
    }

    @Nonnull
    public List<PsiElement> getControlFlowElements() {
        return myControlFlowElements;
    }
}
