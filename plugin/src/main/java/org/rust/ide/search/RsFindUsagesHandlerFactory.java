/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import consulo.annotation.component.ExtensionImpl;
import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesHandlerFactory;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.ui.MessageBoxBuilder;
import consulo.ui.MessageButtonRole;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.impl.RsAbstractableOwnerUtil;
import org.rust.lang.core.psi.ext.impl.RsAbstractableUtil;
import org.rust.lang.core.psi.ext.RsNamedElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ExtensionImpl
public class RsFindUsagesHandlerFactory extends FindUsagesHandlerFactory {

    @Override
    public boolean canFindUsages(PsiElement element) {
        return element instanceof RsNamedElement;
    }

    @Override
    public FindUsagesHandler createFindUsagesHandler(PsiElement element, boolean forHighlightUsages) {
        List<PsiElement> secondaryElements = forHighlightUsages ? Collections.emptyList() : findSecondaryElements(element);
        return new RsFindUsagesHandler(element, secondaryElements);
    }

    @Override
    @RequiredUIAccess
    public CompletableFuture<@Nullable FindUsagesHandler> createFindUsagesHandlerAsync(PsiElement element, boolean forHighlightUsages) {
        if (forHighlightUsages || !(element instanceof RsAbstractable abstractable)
            || RsAbstractableOwnerUtil.getOwner(abstractable) instanceof RsAbstractableOwner.Trait
            || abstractable.getSuperItem() == null) {
            return CompletableFuture.completedFuture(createFindUsagesHandler(element, forHighlightUsages));
        }

        RsAbstractable superItem = abstractable.getSuperItem();
        return MessageBoxBuilder.<Boolean>create()
            .asQuestion()
            .title(LocalizeValue.of(RsBundle.message("dialog.title.find.usages")))
            .text(LocalizeValue.of(RsBundle.message("dialog.message.do.you.want.to.find.usages.base.declaration")))
            .button(MessageButtonRole.YES, Boolean.TRUE)
            .asDefaultButton()
            .button(MessageButtonRole.NO, Boolean.FALSE)
            .asExitButton()
            .showAsync(element.getProject())
            .thenApply(search -> new RsFindUsagesHandler(element, search ? superItemElements(element, superItem) : Collections.emptyList()));
    }

    private static List<PsiElement> superItemElements(PsiElement element, RsAbstractable superItem) {
        List<PsiElement> result = new ArrayList<>(findImplDeclarations(superItem));
        result.remove(element);
        result.add(superItem);
        return result;
    }

    private static List<PsiElement> findSecondaryElements(PsiElement element) {
        if (!(element instanceof RsAbstractable)) return Collections.emptyList();
        RsAbstractable abstractable = (RsAbstractable) element;

        RsAbstractableOwner owner = RsAbstractableOwnerUtil.getOwner(abstractable);
        if (owner instanceof RsAbstractableOwner.Trait) {
            return findImplDeclarations(abstractable);
        }

        RsAbstractable superItem = abstractable.getSuperItem();
        if (superItem == null) return Collections.emptyList();

        if (askWhetherShouldSearchForUsagesOfSuperItem(element.getProject())) {
            return superItemElements(element, superItem);
        } else {
            return Collections.emptyList();
        }
    }

    private static List<PsiElement> findImplDeclarations(RsAbstractable declaration) {
        RsAbstractableOwner owner = RsAbstractableOwnerUtil.getOwner(declaration);
        if (!(owner instanceof RsAbstractableOwner.Trait)) return Collections.emptyList();
        java.util.List<RsAbstractable> impls = RsAbstractableUtil.searchForImplementations(declaration);
        return new java.util.ArrayList<>(impls);
    }

    private static boolean askWhetherShouldSearchForUsagesOfSuperItem(Project project) {
        int result = Messages.showYesNoDialog(
            project,
            RsBundle.message("dialog.message.do.you.want.to.find.usages.base.declaration"),
            RsBundle.message("dialog.title.find.usages"),
            Messages.getQuestionIcon()
        );
        return result == Messages.YES;
    }
}
