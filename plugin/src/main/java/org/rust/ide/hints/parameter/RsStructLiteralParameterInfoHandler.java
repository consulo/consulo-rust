/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import consulo.language.editor.parameterInfo.ParameterInfoUIContext;
import consulo.language.editor.parameterInfo.UpdateParameterInfoContext;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.utils.PsiUtils;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.Substitution;

import java.util.*;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.parameterInfo.ParameterInfoContext;
import org.rust.ide.utils.SearchByOffset;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.ext.RsTypeReferenceUtil;

@ExtensionImpl
public class RsStructLiteralParameterInfoHandler extends RsAsyncParameterInfoHandler<RsStructLiteralBody, RsStructLiteralParameterInfoHandler.Description> {

    public static class Description {
        private final Field[] myFields;

        public Description(@Nonnull Field[] fields) {
            this.myFields = fields;
        }

        @Nonnull
        public Field[] getFields() {
            return myFields;
        }
    }

    public static class Field {
        private final String myName;
        private final String myType;

        public Field(@Nonnull String name, @Nonnull String type) {
            this.myName = name;
            this.myType = type;
        }

        @Nonnull
        public String getName() {
            return myName;
        }

        @Nonnull
        public String getType() {
            return myType;
        }
    }

    @Nullable
    @Override
    public RsStructLiteralBody findTargetElement(@Nonnull PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        if (element == null) return null;
        return RsElementUtil.ancestorStrict(element, RsStructLiteralBody.class);
    }

    @Nullable
    @Override
    public Description[] calculateParameterInfo(@Nonnull RsStructLiteralBody element) {
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
    public void updateParameterInfo(@Nonnull RsStructLiteralBody parameterOwner, @Nonnull UpdateParameterInfoContext context) {
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
    private String findCurrentFieldName(@Nonnull RsStructLiteralBody structLiteral, int offset) {
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
    public void updateUI(@Nonnull Description p, @Nonnull ParameterInfoUIContext context) {
        Field[] fields = p.getFields();
        String[] fieldsText = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldsText[i] = fields[i].getName() + ": " + fields[i].getType();
        }
        String text = fieldsText.length == 0 ? "<no fields>" : String.join(", ", fieldsText);
        TextRange range = getArgumentRange(fieldsText, context.getCurrentParameterIndex());
        updateUI(text, range, context);
    }

    @Override
    public Object[] getParametersForLookup(consulo.language.editor.completion.lookup.LookupElement item, consulo.language.editor.parameterInfo.ParameterInfoContext context) {
        return null;
    }

    @Override
    public boolean couldShowInLookup() {
        return false;
    }

    @jakarta.annotation.Nonnull
    @Override
    public consulo.language.Language getLanguage() {
        return org.rust.lang.RsLanguage.INSTANCE;
    }
}
