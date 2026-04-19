/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.List;
import org.rust.lang.core.psi.ext.RsElement;

public class PresentationInfo {

    @Nullable
    private final String myType;
    @NotNull
    private final String myName;
    @NotNull
    private final DeclarationInfo myDeclaration;
    @NotNull
    private final String myLocation;

    public PresentationInfo(@NotNull RsNamedElement element, @Nullable String type, @NotNull String name, @NotNull DeclarationInfo declaration) {
        myType = type;
        myName = name;
        myDeclaration = declaration;
        myLocation = element.getContainingFile() != null ? " [" + element.getContainingFile().getName() + "]" : "";
    }

    @Nullable
    public String getType() {
        return myType;
    }

    @NotNull
    public String getName() {
        return myName;
    }

    @NotNull
    public String getProjectStructureItemText() {
        return myName + myDeclaration.getSuffix();
    }

    @NotNull
    public String getSignatureText() {
        return myDeclaration.getPrefix() + "<b>" + myName + "</b>" + OpenApiUtil.escaped(myDeclaration.getSuffix());
    }

    @NotNull
    public String getQuickDocumentationText() {
        String prefix = "";
        if (myDeclaration.isAmbiguous() && myType != null) {
            prefix = "<i>" + myType + ":</i> ";
        }
        String valueText = myDeclaration.getValue().isEmpty() ? "" : " " + myDeclaration.getValue();
        return prefix + getSignatureText() + OpenApiUtil.escaped(valueText) + myLocation;
    }

    @Nullable
    public static PresentationInfo getPresentationInfo(@NotNull RsNamedElement element) {
        String elementName = element.getName();
        if (elementName == null) return null;

        String type;
        DeclarationInfo declInfo;

        if (element instanceof RsFunction) {
            RsFunction fn = (RsFunction) element;
            type = "function";
            List<PsiElement> stopAt = new ArrayList<>();
            stopAt.add(fn.getWhereClause());
            stopAt.add(fn.getRetType());
            stopAt.add(fn.getValueParameterList());
            declInfo = createDeclarationInfo(fn, fn.getIdentifier(), false, stopAt, null);
        } else if (element instanceof RsStructItem) {
            RsStructItem struct = (RsStructItem) element;
            type = "struct";
            List<PsiElement> stopAt = new ArrayList<>();
            if (struct.getBlockFields() != null) {
                stopAt.add(struct.getWhereClause());
            } else {
                stopAt.add(struct.getWhereClause());
                stopAt.add(struct.getTupleFields());
            }
            declInfo = createDeclarationInfo(struct, struct.getIdentifier(), false, stopAt, null);
        } else if (element instanceof RsNamedFieldDecl) {
            RsNamedFieldDecl field = (RsNamedFieldDecl) element;
            type = "field";
            List<PsiElement> stopAt = new ArrayList<>();
            stopAt.add(field.getTypeReference());
            declInfo = createDeclarationInfo(field, field.getIdentifier(), false, stopAt, null);
        } else if (element instanceof RsEnumItem) {
            RsEnumItem enumItem = (RsEnumItem) element;
            type = "enum";
            List<PsiElement> stopAt = new ArrayList<>();
            stopAt.add(enumItem.getWhereClause());
            declInfo = createDeclarationInfo(enumItem, enumItem.getIdentifier(), false, stopAt, null);
        } else if (element instanceof RsEnumVariant) {
            RsEnumVariant variant = (RsEnumVariant) element;
            type = "enum variant";
            List<PsiElement> stopAt = new ArrayList<>();
            stopAt.add(variant.getTupleFields());
            declInfo = createDeclarationInfo(variant, variant.getIdentifier(), false, stopAt, null);
        } else if (element instanceof RsTraitItem) {
            RsTraitItem trait = (RsTraitItem) element;
            type = "trait";
            List<PsiElement> stopAt = new ArrayList<>();
            stopAt.add(trait.getWhereClause());
            declInfo = createDeclarationInfo(trait, trait.getIdentifier(), false, stopAt, null);
        } else if (element instanceof RsTypeAlias) {
            RsTypeAlias alias = (RsTypeAlias) element;
            type = "type alias";
            List<PsiElement> stopAt = new ArrayList<>();
            stopAt.add(alias.getTypeReference());
            stopAt.add(alias.getTypeParamBounds());
            stopAt.add(alias.getWhereClause());
            stopAt.add(alias.getTypeParameterList());
            declInfo = createDeclarationInfo(alias, alias.getIdentifier(), false, stopAt, alias.getEq());
        } else if (element instanceof RsConstant) {
            RsConstant constant = (RsConstant) element;
            type = "constant";
            List<PsiElement> stopAt = new ArrayList<>();
            stopAt.add(constant.getExpr());
            stopAt.add(constant.getTypeReference());
            declInfo = createDeclarationInfo(constant, constant.getIdentifier(), false, stopAt, constant.getEq());
        } else if (element instanceof RsTypeParameter) {
            type = "type parameter";
            declInfo = createDeclarationInfo((RsElement) element, ((RsTypeParameter) element).getIdentifier(), true, new ArrayList<>(), null);
        } else if (element instanceof RsModItem) {
            type = "module";
            declInfo = createDeclarationInfo((RsElement) element, ((RsModItem) element).getIdentifier(), false, new ArrayList<>(), null);
        } else if (element instanceof RsFile) {
            RsFile file = (RsFile) element;
            String mName = file.getModName();
            if (file.isCrateRoot()) {
                return new PresentationInfo(element, "crate", "crate", new DeclarationInfo());
            }
            if (mName != null) {
                return new PresentationInfo(element, "module ", mName, new DeclarationInfo("mod "));
            }
            type = "file";
            declInfo = new DeclarationInfo();
        } else {
            type = element.getClass().getSimpleName();
            PsiElement nameId = element instanceof RsNameIdentifierOwner ? ((RsNameIdentifierOwner) element).getNameIdentifier() : null;
            declInfo = createDeclarationInfo((RsElement) element, nameId, true, new ArrayList<>(), null);
        }

        if (declInfo == null) return null;
        return new PresentationInfo(element, type, elementName, declInfo);
    }

    @Nullable
    private static DeclarationInfo createDeclarationInfo(
        @NotNull RsElement decl,
        @Nullable PsiElement name,
        boolean isAmbiguous,
        @NotNull List<PsiElement> stopAt,
        @Nullable PsiElement valueSeparator
    ) {
        if (name == null) return null;

        PsiElement child = decl.getFirstChild();
        int signatureStart = -1;
        while (child != null) {
            if (!(child instanceof PsiWhiteSpace) && !(child instanceof PsiComment) && !(child instanceof RsOuterAttr)) {
                signatureStart = child.getStartOffsetInParent();
                break;
            }
            child = child.getNextSibling();
        }
        if (signatureStart == -1) return null;

        int nameStart = offsetIn(name, decl);
        int nameEnd = nameStart + name.getTextLength();

        int end = nameEnd;
        for (PsiElement e : stopAt) {
            if (e != null) {
                end = e.getStartOffsetInParent() + e.getTextLength();
                break;
            }
        }

        int valueStart = valueSeparator != null ? offsetIn(valueSeparator, decl) : end;

        String text = decl.getText();
        if (signatureStart > nameStart || nameEnd > valueStart || valueStart > end || end > text.length()) {
            return null;
        }

        String prefix = OpenApiUtil.escaped(text.substring(signatureStart, nameStart));
        String value = text.substring(valueStart, end);
        String suffix = text.substring(nameEnd, end - value.length())
            .replaceAll("\\s+", " ")
            .replace("( ", "(")
            .replace(" )", ")")
            .replace(" ,", ",")
            .stripTrailing();

        return new DeclarationInfo(prefix, suffix, value, isAmbiguous);
    }

    private static int offsetIn(@NotNull PsiElement element, @NotNull PsiElement owner) {
        int offset = 0;
        PsiElement current = element;
        while (current != owner && current != null) {
            offset += current.getStartOffsetInParent();
            current = current.getParent();
        }
        return offset;
    }
}
