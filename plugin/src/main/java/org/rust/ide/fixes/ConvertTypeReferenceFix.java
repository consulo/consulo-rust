/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier;
import consulo.codeEditor.Editor;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.types.ty.Ty;

public class ConvertTypeReferenceFix extends RsQuickFixBase<RsTypeReference> {

    
    private final String identifier;

    @FileModifier.SafeFieldForPreview
    private final Ty ty;

    public ConvertTypeReferenceFix(@Nonnull RsTypeReference reference, @Nonnull  String identifier, @Nonnull Ty ty) {
        super(reference);
        this.identifier = identifier;
        this.ty = ty;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.convert.type"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.change.type.to2", identifier, TypeRendering.render(ty)));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsTypeReference element) {
        RsPsiFactory factory = new RsPsiFactory(project);
        RsTypeReference type = factory.tryCreateType(TypeRendering.renderInsertionSafe(ty));
        if (type == null) return;
        element.replace(type);
    }
}
