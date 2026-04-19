/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractEnumVariant;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.refactoring.RsBaseEditorRefactoringAction;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.core.FeatureAvailability;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsEnumVariantUtil;

public class RsExtractEnumVariantAction extends RsBaseEditorRefactoringAction {

    @Override
    public boolean isAvailableOnElementInEditorAndFile(
        @NotNull PsiElement element,
        @NotNull Editor editor,
        @NotNull PsiFile file,
        @NotNull DataContext context
    ) {
        return findApplicableContext(editor, file) != null;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @Nullable DataContext dataContext) {
        RsEnumVariant ctx = findApplicableContext(editor, file);
        if (ctx == null) return;
        RsExtractEnumVariantProcessor processor = new RsExtractEnumVariantProcessor(project, editor, ctx);
        processor.setPreviewUsages(false);
        processor.run();
    }

    @Nullable
    private static RsEnumVariant findApplicableContext(@NotNull Editor editor, @NotNull PsiFile file) {
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
