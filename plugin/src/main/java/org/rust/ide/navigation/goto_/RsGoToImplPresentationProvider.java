/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.language.editor.ui.navigation.PsiTargetPresentationFactory;
import consulo.language.editor.ui.navigation.TargetPresentationProvider;
import consulo.language.psi.PsiElement;
import consulo.navigation.TargetPresentation;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.impl.RsAbstractableImplUtil;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;

/**
 * Shows an abstractable member under its owning impl or trait instead of under the member itself.
 */
public class RsGoToImplPresentationProvider implements TargetPresentationProvider<PsiElement> {
    public static final RsGoToImplPresentationProvider INSTANCE = new RsGoToImplPresentationProvider();

    @Nonnull
    @Override
    @RequiredReadAction
    public TargetPresentation getPresentation(PsiElement element) {
        return Application.get().getInstance(PsiTargetPresentationFactory.class).presentation(getTarget(element));
    }

    @Nonnull
    private static PsiElement getTarget(@Nonnull PsiElement element) {
        if (element instanceof RsAbstractable abstractable) {
            RsAbstractableOwner owner = RsAbstractableImplUtil.getOwner(abstractable);
            if (owner instanceof RsAbstractableOwner.Impl impl) {
                return impl.getImpl();
            }
            if (owner instanceof RsAbstractableOwner.Trait trait) {
                return trait.getTrait();
            }
        }
        return element;
    }
}
