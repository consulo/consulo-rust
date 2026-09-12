/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate.getter;

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
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.RsTypesUtil;
// import org.rust.lang.core.types.ImplLookupUtil; // placeholder
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.ty.TyPrimitive;
import org.rust.lang.core.types.ty.TyUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.List;
import consulo.language.Language;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.infer.FoldUtil;

public class GenerateGetterHandler extends GenerateAccessorHandler {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    @Nonnull
    @Override
    protected String getDialogTitle() {
        return "Select Fields to Generate Getters";
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

    @Nonnull
    @Override
    public String methodName(@Nonnull StructMember member) {
        return member.getArgumentIdentifier();
    }

    @Nonnull
    private static String[] getBorrowAndType(@Nonnull Ty type, @Nonnull String typeReferenceText, @Nonnull RsElement context) {
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
