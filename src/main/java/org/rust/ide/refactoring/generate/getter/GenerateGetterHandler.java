/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate.getter;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.refactoring.generate.GenerateAccessorHandler;
import org.rust.ide.refactoring.generate.StructMember;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.RsTypesUtil;
// import org.rust.lang.core.types.ImplLookupUtil; // placeholder
import org.rust.lang.core.types.infer.SubstituteUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.TyPrimitive;
import org.rust.lang.core.types.ty.TyUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.List;

public class GenerateGetterHandler extends GenerateAccessorHandler {

    @NotNull
    @Override
    protected String getDialogTitle() {
        return "Select Fields to Generate Getters";
    }

    @Nullable
    @Override
    protected List<RsFunction> generateAccessors(
        @NotNull RsStructItem struct,
        @Nullable RsImplItem implBlock,
        @NotNull List<StructMember> chosenFields,
        @NotNull Substitution substitution,
        @NotNull Editor editor
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
            if (member.getField().getTypeReference() == null) continue;
            Ty fieldType = org.rust.lang.core.types.infer.FoldUtil.substitute(RsTypesUtil.getRawType(member.getField().getTypeReference()), substitution);

            String[] borrowAndType = getBorrowAndType(fieldType, member.getTypeReferenceText(), (RsElement) member.getField());
            String borrow = borrowAndType[0];
            String typeStr = borrowAndType[1];
            String fnSignature = "pub fn " + fieldName + "(&self) -> " + borrow + typeStr;
            String fnBody = borrow + "self." + fieldName;

            RsFunction accessor = new RsPsiFactory(project).createTraitMethodMember(fnSignature + " {\n" + fnBody + "\n}");
            RsFunction inserted = (RsFunction) impl.getMembers().addBefore(accessor, impl.getMembers().getRbrace());
            if (inserted != null) {
                result.add(inserted);
            }
        }
        return result;
    }

    @NotNull
    @Override
    public String methodName(@NotNull StructMember member) {
        return member.getArgumentIdentifier();
    }

    @NotNull
    private static String[] getBorrowAndType(@NotNull Ty type, @NotNull String typeReferenceText, @NotNull RsElement context) {
        if (type instanceof TyPrimitive) {
            return new String[]{"", typeReferenceText};
        }
        if (type instanceof TyAdt) {
            TyAdt tyAdt = (TyAdt) type;
            if (tyAdt.getItem().equals(KnownItems.getKnownItems(tyAdt.getItem()).getString())) {
                return new String[]{"&", "str"};
            }
            if (!TyUtil.isMovesByDefault(type, org.rust.lang.core.types.ExtensionsUtil.getImplLookup(context))) {
                return new String[]{"", typeReferenceText};
            }
            return new String[]{"&", typeReferenceText};
        }
        if (!TyUtil.isMovesByDefault(type, org.rust.lang.core.types.ExtensionsUtil.getImplLookup(context))) {
            return new String[]{"", typeReferenceText};
        }
        return new String[]{"&", typeReferenceText};
    }
}
