/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractStructFields;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.RsBaseEditorRefactoringAction;
import org.rust.ide.refactoring.generate.StructMember;
import org.rust.ide.refactoring.generate.StructMemberChooserUi;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsStructItemUtil;
import org.rust.lang.core.types.TyFingerprint;

import java.util.List;
import org.rust.lang.core.types.SubstitutionUtil;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionRef;

@ActionImpl(
    id = "Rust.RsExtractStructFields",
    parents = @ActionParentRef(
        value = @ActionRef(id = "IntroduceActionsGroup"),
        anchor = ActionRefAnchor.AFTER,
        relatedToAction = @ActionRef(id = "Rust.RsExtractEnumVariant")
    )
)
public class RsExtractStructFieldsAction extends RsBaseEditorRefactoringAction {

    @Override
    public boolean isAvailableOnElementInEditorAndFile(
        @Nonnull PsiElement element,
        @Nonnull Editor editor,
        @Nonnull PsiFile file,
        @Nonnull DataContext context
    ) {
        return findApplicableContext(editor, file) != null;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, @Nullable DataContext dataContext) {
        RsStructItem struct = findApplicableContext(editor, file);
        if (struct == null) return;
        List<StructMember> fields = StructMember.fromStruct(struct, org.rust.lang.core.types.SubstitutionUtil.getEmptySubstitution());

        List<StructMember> chosenFields = StructMemberChooserUi.showStructMemberChooserDialog(
            project,
            struct,
            fields,
            RsBundle.message("action.Rust.RsExtractStructFields.choose.fields.title"),
            false
        );
        if (chosenFields == null) return;
        if (chosenFields.isEmpty()) return;

        String name = ExtractFieldsUiUtils.showExtractStructFieldsDialog(project);
        if (name == null) return;
        RsExtractStructFieldsContext ctx = new RsExtractStructFieldsContext(struct, chosenFields, name);
        RsExtractStructFieldsProcessor processor = new RsExtractStructFieldsProcessor(project, editor, ctx);
        processor.setPreviewUsages(false);
        processor.run();
    }

    @Nullable
    private static RsStructItem findApplicableContext(@Nonnull Editor editor, @Nonnull PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement element = file.findElementAt(offset);
        if (element == null) return null;
        RsStructItem struct = RsElementUtil.ancestorOrSelf(element, RsStructItem.class);
        if (struct == null) return null;
        if (RsStructItemUtil.isTupleStruct(struct)) return null;
        if (struct.getBlockFields() == null) return null;
        if (struct.getBlockFields().getNamedFieldDeclList().isEmpty()) return null;
        return struct;
    }
}
