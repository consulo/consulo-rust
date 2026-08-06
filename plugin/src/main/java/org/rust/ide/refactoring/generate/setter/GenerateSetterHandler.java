/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate.setter;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.refactoring.generate.GenerateAccessorHandler;
import org.rust.ide.refactoring.generate.StructMember;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.types.Substitution;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.List;

public class GenerateSetterHandler extends GenerateAccessorHandler {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    @Nonnull
    @Override
    protected String getDialogTitle() {
        return "Select Fields to Generate Setters";
    }

    @Nullable
    @Override
    protected List<RsFunction> generateAccessors(
        @Nonnull RsStructItem struct,
        @Nullable RsImplItem implBlock,
        @Nonnull List<StructMember> chosenFields,
        @Nonnull Substitution substitution,
        @Nonnull Editor editor
    ) {
        org.rust.openapiext.OpenApiUtil.checkWriteAccessAllowed();
        Project project = editor.getProject();
        if (project == null) return null;
        String structName = struct.getName();
        if (structName == null) return null;
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        RsImplItem impl = getOrCreateImplBlock(implBlock, psiFactory, structName, struct);

        List<RsFunction> result = new ArrayList<>();
        for (StructMember member : chosenFields) {
            String fieldName = member.getArgumentIdentifier();
            String typeStr = member.getTypeReferenceText();

            String fnSignature = "pub fn " + methodName(member) + "(&mut self, " + fieldName + ": " + typeStr + ")";
            String fnBody = "self." + fieldName + " = " + fieldName + ";";

            RsFunction accessor = new RsPsiFactory(project).createTraitMethodMember(fnSignature + " {\n" + fnBody + "\n}");
            RsFunction inserted = (RsFunction) impl.getMembers().addBefore(accessor, impl.getMembers().getRbrace());
            if (inserted != null) {
                result.add(inserted);
            }
        }
        return result;
    }

    @Nonnull
    @Override
    public String methodName(@Nonnull StructMember member) {
        return "set_" + member.getArgumentIdentifier();
    }
}
