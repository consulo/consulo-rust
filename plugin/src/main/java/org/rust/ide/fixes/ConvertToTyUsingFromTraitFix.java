/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier.SafeFieldForPreview;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;
import consulo.language.editor.inspection.FileModifier;

/**
 * For the given {@code expr} converts it to the type {@code ty} with {@code ty::from(expr)}
 */
public class ConvertToTyUsingFromTraitFix extends ConvertToTyUsingTraitFix {
    @SafeFieldForPreview
    private final Ty myTy;

    public ConvertToTyUsingFromTraitFix(@Nonnull RsExpr expr, @Nonnull Ty ty) {
        super(expr, ty, "From");
        this.myTy = ty;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
        RsExpr newElement = new RsPsiFactory(project).createAssocFunctionCall(
            TypeRendering.render(myTy, false),
            "from",
            Collections.singletonList(element)
        );
        element.replace(newElement);
    }
}
