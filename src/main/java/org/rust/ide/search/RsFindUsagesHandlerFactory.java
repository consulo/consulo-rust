/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.RsAbstractableOwnerUtil;
import org.rust.lang.core.psi.ext.RsAbstractableUtil;
import org.rust.lang.core.psi.ext.RsNamedElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            List<PsiElement> result = new ArrayList<>(findImplDeclarations(superItem));
            result.remove(element);
            result.add(superItem);
            return result;
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
