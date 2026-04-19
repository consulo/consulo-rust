/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.presentation.PsiRenderingOptions;
import org.rust.ide.presentation.TypeSubstitutingPsiRenderer;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsFieldDecl;
import org.rust.lang.core.psi.ext.RsFieldsOwnerUtil;
import org.rust.lang.core.psi.ext.RsStructItemUtil;
import org.rust.lang.core.types.Substitution;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StructMember {
    @NotNull
    private final String myArgumentIdentifier;
    @NotNull
    private final String myFieldIdentifier;
    @NotNull
    private final String myTypeReferenceText;
    @NotNull
    private final RsFieldDecl myField;

    public StructMember(
        @NotNull String argumentIdentifier,
        @NotNull String fieldIdentifier,
        @NotNull String typeReferenceText,
        @NotNull RsFieldDecl field
    ) {
        myArgumentIdentifier = argumentIdentifier;
        myFieldIdentifier = fieldIdentifier;
        myTypeReferenceText = typeReferenceText;
        myField = field;
    }

    @NotNull
    public String getArgumentIdentifier() {
        return myArgumentIdentifier;
    }

    @NotNull
    public String getFieldIdentifier() {
        return myFieldIdentifier;
    }

    @NotNull
    public String getTypeReferenceText() {
        return myTypeReferenceText;
    }

    @NotNull
    public RsFieldDecl getField() {
        return myField;
    }

    @NotNull
    public String getDialogRepresentation() {
        return myArgumentIdentifier + ": " + myTypeReferenceText;
    }

    @NotNull
    public static List<StructMember> fromStruct(@NotNull RsStructItem structItem, @NotNull Substitution substitution) {
        if (RsStructItemUtil.isTupleStruct(structItem)) {
            return fromTupleList(org.rust.lang.core.psi.ext.RsFieldsOwnerExtUtil.getPositionalFields(structItem), substitution);
        } else {
            return fromFieldList(RsFieldsOwnerUtil.getNamedFields(structItem), substitution);
        }
    }

    @NotNull
    private static List<StructMember> fromTupleList(
        @NotNull List<RsTupleFieldDecl> tupleFieldList,
        @NotNull Substitution substitution
    ) {
        List<StructMember> result = new ArrayList<>();
        for (int i = 0; i < tupleFieldList.size(); i++) {
            RsTupleFieldDecl tupleField = tupleFieldList.get(i);
            String typeName = renderTypeReference(tupleField.getTypeReference(), substitution);
            result.add(new StructMember("field" + i, "()", typeName, tupleField));
        }
        return result;
    }

    @NotNull
    private static List<StructMember> fromFieldList(
        @NotNull List<RsNamedFieldDecl> fieldDeclList,
        @NotNull Substitution substitution
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

    @NotNull
    private static String renderTypeReference(@NotNull RsTypeReference typeReference, @NotNull Substitution substitution) {
        TypeSubstitutingPsiRenderer renderer = new TypeSubstitutingPsiRenderer(
            new PsiRenderingOptions(false),
            substitution
        );
        return org.rust.ide.presentation.RsPsiRendererUtil.renderTypeReference(renderer, typeReference);
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
