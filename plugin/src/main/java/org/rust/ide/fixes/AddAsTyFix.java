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
import org.rust.RsBundle;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.types.ty.Ty;
import consulo.localize.LocalizeValue;

/**
 * For the given {@code expr} adds cast to the given type {@code ty}
 */
public class AddAsTyFix extends RsQuickFixBase<RsExpr> {
    @SafeFieldForPreview
    private final Ty ty;

    public AddAsTyFix(@Nonnull RsExpr expr, @Nonnull Ty ty) {
        super(expr);
        this.ty = ty;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.add.safe.cast"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.safe.cast.to", TypeRendering.getShortPresentableText(ty)));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
        element.replace(new RsPsiFactory(project).createCastExpr(element, TypeRendering.renderInsertionSafe(ty, element)));
    }
}
