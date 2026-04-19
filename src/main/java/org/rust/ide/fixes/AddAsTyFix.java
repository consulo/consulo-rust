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
import org.rust.RsBundle;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.types.ty.Ty;

/**
 * For the given {@code expr} adds cast to the given type {@code ty}
 */
public class AddAsTyFix extends RsQuickFixBase<RsExpr> {
    @SafeFieldForPreview
    private final Ty ty;

    public AddAsTyFix(@NotNull RsExpr expr, @NotNull Ty ty) {
        super(expr);
        this.ty = ty;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.add.safe.cast");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.safe.cast.to", TypeRendering.getShortPresentableText(ty));
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsExpr element) {
        element.replace(new RsPsiFactory(project).createCastExpr(element, TypeRendering.renderInsertionSafe(ty, element)));
    }
}
