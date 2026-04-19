/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractTrait;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.ui.RefactoringDialog;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsMemberInfo;
import org.rust.ide.refactoring.RsMemberSelectionPanel;
import org.rust.ide.refactoring.RsNamesValidator;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RsExtractTraitHandler implements RefactoringActionHandler {
    @TestOnly
    public static final Key<Boolean> RS_EXTRACT_TRAIT_MEMBER_IS_SELECTED = Key.create("RS_EXTRACT_TRAIT_MEMBER_IS_SELECTED");

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @Nullable DataContext dataContext) {
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

    private void invokeInUnitTestMode(@NotNull RsTraitOrImpl traitOrImpl) {
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
    public void invoke(@NotNull Project project, @NotNull PsiElement[] elements, @Nullable DataContext dataContext) {
        /* not called from the editor */
    }
}
