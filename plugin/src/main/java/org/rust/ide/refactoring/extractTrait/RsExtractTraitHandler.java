/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractTrait;

import consulo.dataContext.DataContext;
import consulo.undoRedo.CommandProcessor;
import consulo.logging.Logger;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.ui.RefactoringDialog;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.RsBundle;
import org.rust.ide.refactoring.RsMemberInfo;
import org.rust.ide.refactoring.RsMemberSelectionPanel;
import org.rust.lang.core.names.RsNamesValidator;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import org.rust.lang.core.psi.ext.impl.*;

public class RsExtractTraitHandler implements RefactoringActionHandler {
    
    public static final Key<Boolean> RS_EXTRACT_TRAIT_MEMBER_IS_SELECTED = Key.create("RS_EXTRACT_TRAIT_MEMBER_IS_SELECTED");

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nullable DataContext dataContext) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element == null) return;
        RsTraitOrImpl traitOrImpl = RsElementUtil.ancestorOrSelf(element, RsTraitOrImpl.class);
        if (traitOrImpl == null) return;
        if (traitOrImpl instanceof RsImplItem && ((RsImplItem) traitOrImpl).getTraitRef() != null) return;
        if (traitOrImpl instanceof RsTraitItem && ((RsTraitItem) traitOrImpl).getTypeParameterList() != null) return;
        if (!CommonRefactoringUtil.checkReadOnlyStatus(project, traitOrImpl)) return;

        RsMembers members = traitOrImpl.getMembers();
        if (members == null) return;
        List<RsItemElement> itemElements = PsiElementUtil.childrenOfType(members, RsItemElement.class);
        if (itemElements.isEmpty()) return;
        List<RsMemberInfo> memberInfos = itemElements.stream()
            .map(it -> new RsMemberInfo(it, false))
            .collect(Collectors.toList());

        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            invokeInUnitTestMode(traitOrImpl);
        } else {
            RsExtractTraitDialog dialog = new RsExtractTraitDialog(project, traitOrImpl, memberInfos);
            dialog.show();
        }
    }

    private void invokeInUnitTestMode(@Nonnull RsTraitOrImpl traitOrImpl) {
        RsMembers members = traitOrImpl.getMembers();
        List<RsItemElement> allMembers = members != null
            ? PsiElementUtil.childrenOfType(members, RsItemElement.class)
            : new ArrayList<>();
        List<RsItemElement> selectedMembers = allMembers.stream()
            .filter(it -> it.getUserData(RS_EXTRACT_TRAIT_MEMBER_IS_SELECTED) != null)
            .collect(Collectors.toList());
        RsExtractTraitProcessor processor = new RsExtractTraitProcessor(traitOrImpl, "Trait", selectedMembers);
        processor.run();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, @Nullable DataContext dataContext) {
        /* not called from the editor */
    }
}
