/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.utils.PsiUtils;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ref.RsResolveExtUtil;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.Substitution;

import java.util.*;
import org.rust.lang.core.resolve.ref.RsReferenceExtUtil;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;

public class RsStructLiteralParameterInfoHandler extends RsAsyncParameterInfoHandler<RsStructLiteralBody, RsStructLiteralParameterInfoHandler.Description> {

    public static class Description {
        private final Field[] myFields;

        public Description(@NotNull Field[] fields) {
            this.myFields = fields;
        }

        @NotNull
        public Field[] getFields() {
            return myFields;
        }
    }

    public static class Field {
        private final String myName;
        private final String myType;

        public Field(@NotNull String name, @NotNull String type) {
            this.myName = name;
            this.myType = type;
        }

        @NotNull
        public String getName() {
            return myName;
        }

        @NotNull
        public String getType() {
            return myType;
        }
    }

    @Nullable
    @Override
    public RsStructLiteralBody findTargetElement(@NotNull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element == null) return null;
        return RsElementUtil.ancestorStrict(element, RsStructLiteralBody.class);
    }

    @Nullable
    @Override
    public Description[] calculateParameterInfo(@NotNull RsStructLiteralBody element) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof RsStructLiteral)) return null;
        RsStructLiteral structLiteral = (RsStructLiteral) parent;
        PsiElement resolved = RsPathReferenceImpl.deepResolve(structLiteral.getPath().getReference());
        if (!(resolved instanceof RsFieldsOwner)) return null;
        RsFieldsOwner struct = (RsFieldsOwner) resolved;
        if (struct.getBlockFields() == null) return null;

        Object type = RsTypesUtil.getType(structLiteral);
        Substitution subst = type instanceof TyAdt
            ? ((TyAdt) type).getTypeParameterValues()
            : SubstitutionUtil.getEmptySubstitution();

        List<RsNamedFieldDecl> namedFields = RsFieldsOwnerUtil.getNamedFields(struct);
        Field[] fields = new Field[namedFields.size()];
        for (int i = 0; i < namedFields.size(); i++) {
            RsNamedFieldDecl f = namedFields.get(i);
            String name = f.getName() != null ? f.getName() : "";
            String fieldType = f.getTypeReference() != null
                ? org.rust.lang.core.psi.ext.RsTypeReferenceUtil.substAndGetText(f.getTypeReference(), subst)
                : "_";
            fields[i] = new Field(name, fieldType);
        }
        return new Description[]{new Description(fields)};
    }

    @Override
    public void updateParameterInfo(@NotNull RsStructLiteralBody parameterOwner, @NotNull UpdateParameterInfoContext context) {
        Object[] objects = context.getObjectsToView();
        if (objects == null || objects.length != 1) return;
        if (!(objects[0] instanceof Description)) return;
        Description description = (Description) objects[0];

        List<String> declaredFields = new ArrayList<>();
        for (Field f : description.getFields()) {
            declaredFields.add(f.getName());
        }

        Set<String> fields = new HashSet<>();
        for (RsStructLiteralField field : parameterOwner.getStructLiteralFieldList()) {
            String refName = field.getReferenceName();
            if (refName != null) {
                fields.add(refName);
            }
        }

        String currentField = findCurrentFieldName(parameterOwner, context.getOffset());

        int index;
        if (currentField != null) {
            index = declaredFields.indexOf(currentField);
        } else if (declaredFields.size() == fields.size()) {
            index = 0;
        } else {
            index = -1;
            for (int i = 0; i < declaredFields.size(); i++) {
                if (!fields.contains(declaredFields.get(i))) {
                    index = i;
                    break;
                }
            }
        }
        context.setCurrentParameter(index);
    }

    @Nullable
    private String findCurrentFieldName(@NotNull RsStructLiteralBody structLiteral, int offset) {
        PsiFile file = structLiteral.getContainingFile();
        PsiElement element1 = org.rust.ide.utils.SearchByOffset.findElementAtIgnoreWhitespaceBefore(file, offset);
        PsiElement element2 = element1 != null ? RsElementUtil.getPrevNonWhitespaceSibling(element1) : null;
        RsStructLiteralField field = null;
        if (element1 != null) {
            field = RsElementUtil.ancestorOrSelf(element1, RsStructLiteralField.class);
        }
        if (field == null && element2 != null) {
            field = RsElementUtil.ancestorOrSelf(element2, RsStructLiteralField.class);
        }
        if (field == null) return null;
        return field.getReferenceName();
    }

    @Override
    public void updateUI(@NotNull Description p, @NotNull ParameterInfoUIContext context) {
        Field[] fields = p.getFields();
        String[] fieldsText = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldsText[i] = fields[i].getName() + ": " + fields[i].getType();
        }
        String text = fieldsText.length == 0 ? "<no fields>" : String.join(", ", fieldsText);
        TextRange range = getArgumentRange(fieldsText, context.getCurrentParameterIndex());
        updateUI(text, range, context);
    }
}
