/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.language.psi.event.PsiTreeChangeListener;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;

public abstract class RsPsiTreeChangeAdapter implements PsiTreeChangeListener {

    public abstract void handleEvent(@Nonnull RsPsiTreeChangeEvent event);

    @Override
    public void beforePropertyChange(@Nonnull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.PropertyChange.Before(
            event.getPropertyName(),
            event.getOldValue(),
            event.getNewValue(),
            event.getElement(),
            event.getChild()
        ));
    }

    @Override
    public void propertyChanged(@Nonnull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.PropertyChange.After(
            event.getPropertyName(),
            event.getOldValue(),
            event.getNewValue(),
            event.getElement(),
            event.getChild()
        ));
    }

    @Override
    public void beforeChildReplacement(@Nonnull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildReplacement.Before(
            event.getFile(),
            event.getParent(),
            event.getOldChild(),
            event.getNewChild()
        ));
    }

    @Override
    public void childReplaced(@Nonnull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildReplacement.After(
            event.getFile(),
            event.getParent(),
            event.getOldChild(),
            event.getNewChild()
        ));
    }

    @Override
    public void beforeChildAddition(@Nonnull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildAddition.Before(
            event.getFile(),
            event.getParent(),
            event.getChild()
        ));
    }

    @Override
    public void childAdded(@Nonnull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildAddition.After(
            event.getFile(),
            event.getParent(),
            event.getChild()
        ));
    }

    @Override
    public void beforeChildMovement(@Nonnull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildMovement.Before(
            event.getFile(),
            event.getOldParent(),
            event.getNewParent(),
            event.getChild()
        ));
    }

    @Override
    public void childMoved(@Nonnull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildMovement.After(
            event.getFile(),
            event.getOldParent(),
            event.getNewParent(),
            event.getChild()
        ));
    }

    @Override
    public void beforeChildRemoval(@Nonnull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildRemoval.Before(
            event.getFile(),
            event.getParent(),
            event.getChild()
        ));
    }

    @Override
    public void childRemoved(@Nonnull PsiTreeChangeEvent event) {
        handleEvent(new RsPsiTreeChangeEvent.ChildRemoval.After(
            event.getFile(),
            event.getParent(),
            event.getChild()
        ));
    }

    @Override
    public void beforeChildrenChange(@Nonnull PsiTreeChangeEvent event) {
        // PsiTreeChangeEventImpl.isGenericChange() is platform-internal and PsiTreeChangeEvent
        // exposes no equivalent flag. Treating every event as non-generic is the conservative
        // choice: generic events were the ones that could be skipped, so this only costs extra work.
        boolean isGenericChange = false;
        handleEvent(new RsPsiTreeChangeEvent.ChildrenChange.Before(
            event.getFile(),
            event.getParent(),
            isGenericChange
        ));
    }

    @Override
    public void childrenChanged(@Nonnull PsiTreeChangeEvent event) {
        // PsiTreeChangeEventImpl.isGenericChange() is platform-internal and PsiTreeChangeEvent
        // exposes no equivalent flag. Treating every event as non-generic is the conservative
        // choice: generic events were the ones that could be skipped, so this only costs extra work.
        boolean isGenericChange = false;
        handleEvent(new RsPsiTreeChangeEvent.ChildrenChange.After(
            event.getFile(),
            event.getParent(),
            isGenericChange
        ));
    }
}
