/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.types.ty.Ty;

public class ConvertTypeReferenceFix extends RsQuickFixBase<RsTypeReference> {

    @NlsSafe
    private final String identifier;

    @FileModifier.SafeFieldForPreview
    private final Ty ty;

    public ConvertTypeReferenceFix(@NotNull RsTypeReference reference, @NotNull @NlsSafe String identifier, @NotNull Ty ty) {
        super(reference);
        this.identifier = identifier;
        this.ty = ty;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.convert.type");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.change.type.to2", identifier, TypeRendering.render(ty));
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsTypeReference element) {
        RsPsiFactory factory = new RsPsiFactory(project);
        RsTypeReference type = factory.tryCreateType(TypeRendering.renderInsertionSafe(ty));
        if (type == null) return;
        element.replace(type);
    }
}
