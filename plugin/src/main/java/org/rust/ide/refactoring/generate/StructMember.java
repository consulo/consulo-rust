/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.presentation.PsiRenderingOptions;
import org.rust.lang.core.presentation.TypeSubstitutingPsiRenderer;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsFieldDecl;
import org.rust.lang.core.psi.ext.impl.RsFieldsOwnerUtil;
import org.rust.lang.core.psi.ext.impl.RsStructItemUtil;
import org.rust.lang.core.types.Substitution;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.rust.lang.core.presentation.RsPsiRendererUtil;
import org.rust.lang.core.psi.ext.impl.RsFieldsOwnerExtUtil;

public class StructMember {
    @Nonnull
    private final String myArgumentIdentifier;
    @Nonnull
    private final String myFieldIdentifier;
    @Nonnull
    private final String myTypeReferenceText;
    @Nonnull
    private final RsFieldDecl myField;

    public StructMember(
        @Nonnull String argumentIdentifier,
        @Nonnull String fieldIdentifier,
        @Nonnull String typeReferenceText,
        @Nonnull RsFieldDecl field
    ) {
        myArgumentIdentifier = argumentIdentifier;
        myFieldIdentifier = fieldIdentifier;
        myTypeReferenceText = typeReferenceText;
        myField = field;
    }

    @Nonnull
    public String getArgumentIdentifier() {
        return myArgumentIdentifier;
    }

    @Nonnull
    public String getFieldIdentifier() {
        return myFieldIdentifier;
    }

    @Nonnull
    public String getTypeReferenceText() {
        return myTypeReferenceText;
    }

    @Nonnull
    public RsFieldDecl getField() {
        return myField;
    }

    @Nonnull
    public String getDialogRepresentation() {
        return myArgumentIdentifier + ": " + myTypeReferenceText;
    }

    @Nonnull
    public static List<StructMember> fromStruct(@Nonnull RsStructItem structItem, @Nonnull Substitution substitution) {
        if (RsStructItemUtil.isTupleStruct(structItem)) {
            return fromTupleList(org.rust.lang.core.psi.ext.impl.RsFieldsOwnerExtUtil.getPositionalFields(structItem), substitution);
        } else {
            return fromFieldList(RsFieldsOwnerUtil.getNamedFields(structItem), substitution);
        }
    }

    @Nonnull
    private static List<StructMember> fromTupleList(
        @Nonnull List<RsTupleFieldDecl> tupleFieldList,
        @Nonnull Substitution substitution
    ) {
        List<StructMember> result = new ArrayList<>();
        for (int i = 0; i < tupleFieldList.size(); i++) {
            RsTupleFieldDecl tupleField = tupleFieldList.get(i);
            String typeName = renderTypeReference(tupleField.getTypeReference(), substitution);
            result.add(new StructMember("field" + i, "()", typeName, tupleField));
        }
        return result;
    }

    @Nonnull
    private static List<StructMember> fromFieldList(
        @Nonnull List<RsNamedFieldDecl> fieldDeclList,
        @Nonnull Substitution substitution
    ) {
        List<StructMember> result = new ArrayList<>();
        for (RsNamedFieldDecl field : fieldDeclList) {
            String identText = field.getIdentifier().getText();
            String argId = identText != null ? identText : "()";
            String fieldId = identText + ":()";
            String typeText = field.getTypeReference() != null
                ? renderTypeReference(field.getTypeReference(), substitution)
                : "()";
            result.add(new StructMember(argId, fieldId, typeText, field));
        }
        return result;
    }

    @Nonnull
    private static String renderTypeReference(@Nonnull RsTypeReference typeReference, @Nonnull Substitution substitution) {
        TypeSubstitutingPsiRenderer renderer = new TypeSubstitutingPsiRenderer(
            new PsiRenderingOptions(false),
            substitution
        );
        return org.rust.lang.core.presentation.RsPsiRendererUtil.renderTypeReference(renderer, typeReference);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StructMember)) return false;
        StructMember that = (StructMember) o;
        return myArgumentIdentifier.equals(that.myArgumentIdentifier) &&
            myFieldIdentifier.equals(that.myFieldIdentifier) &&
            myTypeReferenceText.equals(that.myTypeReferenceText) &&
            myField.equals(that.myField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myArgumentIdentifier, myFieldIdentifier, myTypeReferenceText, myField);
    }
}
