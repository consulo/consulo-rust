/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.annotator.RsExpressionAnnotator;
import org.rust.ide.utils.template.EditorExtUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.KnownItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public final class StructFieldsExpander {
    private StructFieldsExpander() {
    }

    public static void addMissingFieldsToStructLiteral(
        @NotNull RsPsiFactory factory,
        @Nullable Editor editor,
        @NotNull RsStructLiteral structLiteral,
        boolean recursive
    ) {
        RsFieldsOwner declaration = (RsFieldsOwner) org.rust.lang.core.resolve.ref.RsPathReferenceImpl.deepResolve(structLiteral.getPath().getReference());
        if (declaration == null) return;
        RsStructLiteralBody body = structLiteral.getStructLiteralBody();
        List<RsFieldDecl> fieldsToAdd = RsExpressionAnnotator.calculateMissingFields(body, declaration);
        RsDefaultValueBuilder defaultValueBuilder = new RsDefaultValueBuilder(
            KnownItems.getKnownItems(declaration),
            RsElementUtil.getContainingMod((RsElement) body),
            factory,
            recursive
        );
        List<RsStructLiteralField> addedFields = defaultValueBuilder.fillStruct(
            body,
            declaration.getFields(),
            fieldsToAdd,
            RsPatUtil.getLocalVariableVisibleBindings(structLiteral)
        );
        if (editor != null) {
            List<RsExpr> exprs = new ArrayList<>();
            for (RsStructLiteralField field : addedFields) {
                if (field.getExpr() != null) {
                    exprs.add(field.getExpr());
                }
            }
            EditorExtUtil.buildAndRunTemplate(editor, body, exprs);
        }
    }

    public static void expandStructFields(@NotNull RsPsiFactory factory, @NotNull RsPatStruct patStruct) {
        RsFieldsOwner declaration = (RsFieldsOwner) org.rust.lang.core.resolve.ref.RsPathReferenceImpl.deepResolve(patStruct.getPath().getReference());
        if (declaration == null) return;
        boolean hasTrailingComma = false;
        PsiElement prevSibling = PsiElementUtil.getPrevNonCommentSibling(patStruct.getRbrace());
        if (prevSibling != null) {
            hasTrailingComma = prevSibling.getNode().getElementType() == RsElementTypes.COMMA;
        }
        RsPatRest patRest = patStruct.getPatRest();
        if (patRest != null) {
            patRest.delete();
        }
        List<RsPatField> existingFields = patStruct.getPatFieldList();
        Set<String> bodyFieldNames = existingFields.stream()
            .map(f -> RsPatFieldUtil.getFieldName(RsPatFieldUtil.getKind(f)))
            .collect(Collectors.toSet());
        List<RsPatField> missingFields = new ArrayList<>();
        for (RsFieldDecl field : declaration.getFields()) {
            if (field.getName() != null && !bodyFieldNames.contains(field.getName())) {
                missingFields.add(factory.createPatField(RsRawIdentifiers.escapeIdentifierIfNeeded(field.getName())));
            }
        }

        if (existingFields.isEmpty()) {
            addFieldsToPat(factory, patStruct, missingFields, hasTrailingComma);
            return;
        }

        // Simplified field ordering
        addFieldsToPat(factory, patStruct, missingFields, hasTrailingComma);
    }

    public static void expandTupleStructFields(@NotNull RsPsiFactory factory, @Nullable Editor editor, @NotNull RsPatTupleStruct patTuple) {
        RsFieldsOwner declaration = (RsFieldsOwner) org.rust.lang.core.resolve.ref.RsPathReferenceImpl.deepResolve(patTuple.getPath().getReference());
        if (declaration == null) return;
        boolean hasTrailingComma = false;
        PsiElement prevSibling = PsiElementUtil.getPrevNonCommentSibling(patTuple.getRparen());
        if (prevSibling != null) {
            hasTrailingComma = prevSibling.getNode().getElementType() == RsElementTypes.COMMA;
        }
        List<RsPatIdent> bodyFields = PsiElementUtil.childrenOfType(patTuple, RsPatIdent.class);
        int missingFieldsAmount = declaration.getFields().size() - bodyFields.size();
        List<RsPatBinding> missingFields = new ArrayList<>();
        for (int i = 0; i < missingFieldsAmount; i++) {
            missingFields.add(factory.createPatBinding("_" + i));
        }
        addFieldsToPat(factory, patTuple, new ArrayList<>(missingFields), hasTrailingComma);
        List<RsPatRest> patRests = PsiElementUtil.childrenOfType(patTuple, RsPatRest.class);
        if (!patRests.isEmpty()) {
            patRests.get(0).delete();
        }
        if (editor != null) {
            List<RsPatBinding> bindings = PsiElementUtil.childrenOfType(patTuple, RsPatBinding.class);
            EditorExtUtil.buildAndRunTemplate(editor, patTuple, bindings);
        }
    }

    private static void addFieldsToPat(
        @NotNull RsPsiFactory factory,
        @NotNull RsPat pat,
        @NotNull List<? extends PsiElement> fields,
        boolean hasTrailingComma
    ) {
        PsiElement anchor = determineOrCreateAnchor(factory, pat);
        for (int i = 0; i < fields.size(); i++) {
            PsiElement missingField = fields.get(i);
            pat.addAfter(missingField, anchor);
            if (i == fields.size() - 1) {
                PsiElement nextSibling = anchor.getNextSibling();
                PsiElement nextNonComment = nextSibling != null ? PsiElementUtil.getNextNonCommentSibling(nextSibling) : null;
                if (!(nextNonComment instanceof RsPatRest)) {
                    pat.addAfter(factory.createComma(), anchor.getNextSibling());
                }
            } else {
                pat.addAfter(factory.createComma(), anchor.getNextSibling());
            }
            anchor = anchor.getNextSibling().getNextSibling();
        }
        if (!hasTrailingComma && anchor != null) {
            anchor.delete();
        }
    }

    @NotNull
    private static PsiElement determineOrCreateAnchor(@NotNull RsPsiFactory factory, @NotNull RsPat pat) {
        List<RsPatRest> patRests = PsiElementUtil.childrenOfType(pat, RsPatRest.class);
        RsPatRest patRest = patRests.isEmpty() ? null : patRests.get(0);
        if (patRest != null) {
            return PsiElementUtil.getPrevNonCommentSibling(patRest);
        }
        PsiElement lastElementInBody = PsiElementUtil.getPrevNonCommentSibling(pat.getLastChild());
        if (lastElementInBody != null && !(lastElementInBody instanceof LeafPsiElement)) {
            pat.addAfter(factory.createComma(), lastElementInBody);
            return lastElementInBody.getNextSibling();
        }
        return lastElementInBody;
    }
}
