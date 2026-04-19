/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.presentation.TypeRendering;
import org.rust.ide.utils.template.EditorExt;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;

/**
 * Adds type ascription after the given element.
 */
public class AddTypeFix extends RsQuickFixBase<PsiElement> {
    private final String myTypeText;

    public AddTypeFix(@NotNull PsiElement anchor, @NotNull Ty ty) {
        super(anchor);
        this.myTypeText = TypeRendering.renderInsertionSafe(ty);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.add.type");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.type", myTypeText);
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        RsPsiFactory factory = new RsPsiFactory(project);
        PsiElement parent = element.getParent();

        PsiElement colon = factory.createColon();
        PsiElement anchor = parent.addAfter(colon, element);

        RsTypeReference type = factory.createType(myTypeText);
        PsiElement insertedType = parent.addAfter(type, anchor);

        if (editor != null) {
            EditorExt.buildAndRunTemplate(editor, parent, Collections.singletonList(insertedType));
        }
    }
}
