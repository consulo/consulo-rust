/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.psi.PsiTreeChangeListener;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import org.jetbrains.annotations.NotNull;

public abstract class RsPsiTreeChangeAdapter implements PsiTreeChangeListener {

    public abstract void handleEvent(@NotNull RsPsiTreeChangeEvent event);

    @Override
    public void beforePropertyChange(@NotNull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.PropertyChange.Before(
            event.getPropertyName(),
            event.getOldValue(),
            event.getNewValue(),
            event.getElement(),
            event.getChild()
        ));
    }

    @Override
    public void propertyChanged(@NotNull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.PropertyChange.After(
            event.getPropertyName(),
            event.getOldValue(),
            event.getNewValue(),
            event.getElement(),
            event.getChild()
        ));
    }

    @Override
    public void beforeChildReplacement(@NotNull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildReplacement.Before(
            event.getFile(),
            event.getParent(),
            event.getOldChild(),
            event.getNewChild()
        ));
    }

    @Override
    public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildReplacement.After(
            event.getFile(),
            event.getParent(),
            event.getOldChild(),
            event.getNewChild()
        ));
    }

    @Override
    public void beforeChildAddition(@NotNull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildAddition.Before(
            event.getFile(),
            event.getParent(),
            event.getChild()
        ));
    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildAddition.After(
            event.getFile(),
            event.getParent(),
            event.getChild()
        ));
    }

    @Override
    public void beforeChildMovement(@NotNull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildMovement.Before(
            event.getFile(),
            event.getOldParent(),
            event.getNewParent(),
            event.getChild()
        ));
    }

    @Override
    public void childMoved(@NotNull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildMovement.After(
            event.getFile(),
            event.getOldParent(),
            event.getNewParent(),
            event.getChild()
        ));
    }

    @Override
    public void beforeChildRemoval(@NotNull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildRemoval.Before(
            event.getFile(),
            event.getParent(),
            event.getChild()
        ));
    }

    @Override
    public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildRemoval.After(
            event.getFile(),
            event.getParent(),
            event.getChild()
        ));
    }

    @Override
    public void beforeChildrenChange(@NotNull PsiTreeChangeEvent event) {
        boolean isGenericChange = event instanceof PsiTreeChangeEventImpl
            && ((PsiTreeChangeEventImpl) event).isGenericChange();
        handleEvent(new RsPsiTreeChangeEvent.ChildrenChange.Before(
            event.getFile(),
            event.getParent(),
            isGenericChange
        ));
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        boolean isGenericChange = event instanceof PsiTreeChangeEventImpl
            && ((PsiTreeChangeEventImpl) event).isGenericChange();
        handleEvent(new RsPsiTreeChangeEvent.ChildrenChange.After(
            event.getFile(),
            event.getParent(),
            isGenericChange
        ));
    }
}
