/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.presentation.TypeRendering;
import org.rust.ide.utils.template.EditorExt;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;
import consulo.localize.LocalizeValue;

/**
 * Adds type ascription after the given element.
 */
public class AddTypeFix extends RsQuickFixBase<PsiElement> {
    private final String myTypeText;

    public AddTypeFix(@Nonnull PsiElement anchor, @Nonnull Ty ty) {
        super(anchor);
        this.myTypeText = TypeRendering.renderInsertionSafe(ty);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.add.type"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.type", myTypeText));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
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
