/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractEnumVariant;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.refactoring.RsBaseEditorRefactoringAction;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.core.FeatureAvailability;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsEnumVariantUtil;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionRef;

@ActionImpl(
    id = "Rust.RsExtractEnumVariant",
    parents = @ActionParentRef(
        value = @ActionRef(id = "IntroduceActionsGroup"),
        anchor = ActionRefAnchor.AFTER,
        relatedToAction = @ActionRef(id = "ExtractMethod")
    )
)
public class RsExtractEnumVariantAction extends RsBaseEditorRefactoringAction {

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
        RsEnumVariant ctx = findApplicableContext(editor, file);
        if (ctx == null) return;
        RsExtractEnumVariantProcessor processor = new RsExtractEnumVariantProcessor(project, editor, ctx);
        processor.setPreviewUsages(false);
        processor.run();
    }

    @Nullable
    private static RsEnumVariant findApplicableContext(@Nonnull Editor editor, @Nonnull PsiFile file) {
        int offset = editor.getCaretModel().getOffset();
        PsiElement found = file.findElementAt(offset);
        if (found == null) return null;
        RsEnumVariant variant = RsElementUtil.ancestorOrSelf(found, RsEnumVariant.class);
        if (variant == null) return null;
        if (RsEnumVariantUtil.isFieldless(variant)) return null;
        if (variant.getVariantDiscriminant() != null
            && CompilerFeature.getARBITRARY_ENUM_DISCRIMINANT().availability(RsElementUtil.getContainingMod(variant)) != FeatureAvailability.AVAILABLE) {
            return null;
        }
        return variant;
    }
}
