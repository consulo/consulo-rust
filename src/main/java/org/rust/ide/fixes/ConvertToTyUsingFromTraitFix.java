/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;

/**
 * For the given {@code expr} converts it to the type {@code ty} with {@code ty::from(expr)}
 */
public class ConvertToTyUsingFromTraitFix extends ConvertToTyUsingTraitFix {
    @SafeFieldForPreview
    private final Ty myTy;

    public ConvertToTyUsingFromTraitFix(@NotNull RsExpr expr, @NotNull Ty ty) {
        super(expr, ty, "From");
        this.myTy = ty;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsExpr element) {
        RsExpr newElement = new RsPsiFactory(project).createAssocFunctionCall(
            TypeRendering.render(myTy, false),
            "from",
            Collections.singletonList(element)
        );
        element.replace(newElement);
    }
}
