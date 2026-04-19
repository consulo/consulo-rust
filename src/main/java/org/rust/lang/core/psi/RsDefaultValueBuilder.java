/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.fixes.ChangeToFieldShorthandFix;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.ty.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;
import org.rust.lang.core.psi.ext.RsFieldDeclUtil;
import org.rust.lang.core.psi.ext.RsEnumVariantUtil;
import org.rust.lang.core.psi.ext.RsFieldsOwnerUtil;

public class RsDefaultValueBuilder {
    @NotNull
    private final KnownItems myItems;
    @NotNull
    private final RsMod myMod;
    @NotNull
    private final RsPsiFactory myPsiFactory;
    private final boolean myRecursive;

    public RsDefaultValueBuilder(@NotNull KnownItems items, @NotNull RsMod mod,
                                 @NotNull RsPsiFactory psiFactory) {
        this(items, mod, psiFactory, false);
    }

    public RsDefaultValueBuilder(@NotNull KnownItems items, @NotNull RsMod mod,
                                 @NotNull RsPsiFactory psiFactory, boolean recursive) {
        myItems = items;
        myMod = mod;
        myPsiFactory = psiFactory;
        myRecursive = recursive;
    }

    @NotNull
    private RsExpr getDefaultValue() {
        return myPsiFactory.createExpression("()");
    }

    @NotNull
    private RsExpr buildForSmartPtr(@NotNull TyAdt ty, @NotNull Map<String, RsPatBinding> bindings) {
        RsStructOrEnumItemElement item = ty.getItem();
        String name = item.getName();
        Ty parameter = ty.getTypeParameterValues().get(item.getTypeParameters().get(0));
        return myPsiFactory.createAssocFunctionCall(name, "new",
            Collections.singletonList(buildFor(parameter, bindings)));
    }

    @NotNull
    public RsExpr buildFor(@NotNull Ty ty, @NotNull Map<String, RsPatBinding> bindings) {
        if (ty instanceof TyBool) {
            return myPsiFactory.createExpression("false");
        }
        if (ty instanceof TyInteger) {
            return myPsiFactory.createExpression("0");
        }
        if (ty instanceof TyFloat) {
            return myPsiFactory.createExpression("0.0");
        }
        if (ty instanceof TyChar) {
            return myPsiFactory.createExpression("''");
        }
        if (ty instanceof TyReference) {
            TyReference ref = (TyReference) ty;
            if (ref.getReferenced() instanceof TyStr) {
                return myPsiFactory.createExpression("\"\"");
            }
            return myPsiFactory.createRefExpr(
                buildFor(ref.getReferenced(), bindings),
                Collections.singletonList(ref.getMutability())
            );
        }
        if (ty instanceof TyAdt) {
            TyAdt tyAdt = (TyAdt) ty;
            RsStructOrEnumItemElement item = tyAdt.getItem();

            // Check smart pointers
            RsStructOrEnumItemElement[] smartPointers = {
                myItems.getBox(), myItems.getRc(), myItems.getArc(),
                myItems.getCell(), myItems.getRefCell(),
                myItems.getUnsafeCell(), myItems.getMutex()
            };
            for (RsStructOrEnumItemElement ptr : smartPointers) {
                if (item.equals(ptr)) {
                    return buildForSmartPtr(tyAdt, bindings);
                }
            }

            RsExpr defaultExpr = getDefaultValue();
            ImplLookup implLookup = ExtensionsUtil.getImplLookup(myMod);
            if (implLookup.isDefault(ty).isTrue()) {
                defaultExpr = myPsiFactory.createAssocFunctionCall("Default", "default",
                    Collections.emptyList());
            }

            String name = item.getName();

            if (item.equals(myItems.getOption())) {
                return myPsiFactory.createExpression("None");
            }
            if (item.equals(myItems.getString())) {
                return myPsiFactory.createExpression("\"\".to_string()");
            }
            if (item.equals(myItems.getVec())) {
                return myPsiFactory.createExpression("vec![]");
            }

            if (item instanceof RsStructItem) {
                RsStructItem structItem = (RsStructItem) item;
                if (RsStructItemUtil.getKind(structItem) == RsStructKind.STRUCT
                    && RsVisibilityUtil.canBeInstantiatedIn(structItem, myMod)) {
                    if (implLookup.isDefault(ty).isTrue()) {
                        return defaultExpr;
                    }
                    if (structItem.getBlockFields() != null) {
                        RsStructLiteral structLiteral = myPsiFactory.createStructLiteral(name);
                        if (myRecursive) {
                            fillStruct(
                                structLiteral.getStructLiteralBody(),
                                new ArrayList<RsFieldDecl>(RsFieldsOwnerUtil.getNamedFields(structItem)),
                                new ArrayList<RsFieldDecl>(RsFieldsOwnerUtil.getNamedFields(structItem)),
                                bindings
                            );
                        }
                        return structLiteral;
                    }
                    if (structItem.getTupleFields() != null) {
                        List<RsExpr> argExprs;
                        if (myRecursive) {
                            argExprs = new ArrayList<>();
                            for (RsFieldDecl field : RsFieldsOwnerUtil.getPositionalFields(structItem)) {
                                RsTypeReference typeRef = field.getTypeReference();
                                Ty fieldTy = ExtensionsUtil.normType(typeRef, implLookup);
                                argExprs.add(buildFor(fieldTy, bindings));
                            }
                        } else {
                            argExprs = Collections.emptyList();
                        }
                        return myPsiFactory.createFunctionCall(name, argExprs);
                    }
                    return myPsiFactory.createExpression(name);
                }
                return defaultExpr;
            }

            if (item instanceof RsEnumItem) {
                RsEnumItem enumItem = (RsEnumItem) item;
                if (implLookup.isDefault(ty).isTrue()) {
                    return defaultExpr;
                }
                RsEnumBody enumBody = enumItem.getEnumBody();
                if (enumBody != null) {
                    for (RsEnumVariant variant : enumBody.getEnumVariantList()) {
                        if (RsEnumVariantUtil.isFieldless(variant)) {
                            String variantName = variant.getName();
                            if (variantName != null) {
                                return myPsiFactory.createExpression(name + "::" + variantName);
                            }
                        }
                    }
                }
                return defaultExpr;
            }

            return defaultExpr;
        }
        if (ty instanceof TySlice || ty instanceof TyArray) {
            return myPsiFactory.createExpression("[]");
        }
        if (ty instanceof TyTuple) {
            TyTuple tuple = (TyTuple) ty;
            StringBuilder sb = new StringBuilder("(");
            List<Ty> types = tuple.getTypes();
            for (int i = 0; i < types.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(buildFor(types.get(i), bindings).getText());
            }
            sb.append(")");
            return myPsiFactory.createExpression(sb.toString());
        }
        return getDefaultValue();
    }

    @NotNull
    public List<RsStructLiteralField> fillStruct(
        @NotNull RsStructLiteralBody structLiteral,
        @NotNull List<RsFieldDecl> declaredFields,
        @NotNull List<RsFieldDecl> fieldsToAdd,
        @NotNull Map<String, RsPatBinding> bindings
    ) {
        boolean forceMultiLine = structLiteral.getStructLiteralFieldList().isEmpty() && fieldsToAdd.size() > 2;
        boolean isMultiline = forceMultiLine || structLiteral.textContains('\n');

        List<RsStructLiteralField> addedFields = new ArrayList<>();
        for (int idx = 0; idx < fieldsToAdd.size(); idx++) {
            RsFieldDecl fieldDecl = fieldsToAdd.get(idx);
            RsStructLiteralField field = findLocalBinding(fieldDecl, bindings);
            if (field == null) {
                field = specializedCreateStructLiteralField(fieldDecl, bindings);
            }
            if (field == null) continue;

            RsStructLiteralField addBefore = findPlaceToAdd(field, structLiteral.getStructLiteralFieldList(), declaredFields);
            RsStructLiteralField added;
            if (addBefore == null) {
                PsiModificationUtils.ensureTrailingComma(structLiteral.getStructLiteralFieldList());
                added = (RsStructLiteralField) structLiteral.addBefore(field, structLiteral.getRbrace());
                if (isMultiline && idx == fieldsToAdd.size() - 1) {
                    structLiteral.addAfter(myPsiFactory.createComma(), added);
                }
            } else {
                PsiElement comma = structLiteral.addBefore(myPsiFactory.createComma(), addBefore);
                added = (RsStructLiteralField) structLiteral.addBefore(field, comma);
            }
            addedFields.add(added);
        }

        if (forceMultiLine) {
            structLiteral.addAfter(myPsiFactory.createNewline(), structLiteral.getLbrace());
        }

        return addedFields;
    }

    @Nullable
    private RsStructLiteralField findLocalBinding(@NotNull RsFieldDecl fieldDecl,
                                                   @NotNull Map<String, RsPatBinding> bindings) {
        String name = fieldDecl.getName();
        if (name == null) return null;
        RsTypeReference typeRef = fieldDecl.getTypeReference();
        if (typeRef == null) return null;
        Ty type = ExtensionsUtil.getNormType(typeRef);
        RsPatBinding binding = bindings.get(name);
        if (binding == null) return null;
        String escapedName = RsFieldDeclUtil.getEscapedName(fieldDecl);
        if (escapedName == null) return null;

        Ty bindingType = ExtensionsUtil.getType(binding);
        if (type.isEquivalentTo(bindingType)) {
            RsStructLiteralField field = myPsiFactory.createStructLiteralField(escapedName,
                myPsiFactory.createExpression(escapedName));
            ChangeToFieldShorthandFix.applyShorthandInit(field);
            return field;
        }
        if (isRefContainer(type, bindingType)) {
            RsExpr expr = buildReference(type, myPsiFactory.createExpression(escapedName));
            return myPsiFactory.createStructLiteralField(escapedName, expr);
        }
        return null;
    }

    private boolean isRefContainer(@NotNull Ty container, @NotNull Ty type) {
        if (container.equals(type)) return true;
        if (container instanceof TyReference) {
            return isRefContainer(((TyReference) container).getReferenced(), type);
        }
        return false;
    }

    @NotNull
    private RsExpr buildReference(@NotNull Ty type, @NotNull RsExpr expr) {
        if (type instanceof TyReference) {
            TyReference ref = (TyReference) type;
            Ty inner = ref.getReferenced();
            return myPsiFactory.createRefExpr(
                buildReference(inner, expr),
                Collections.singletonList(ref.getMutability())
            );
        }
        return expr;
    }

    @Nullable
    private RsStructLiteralField findPlaceToAdd(
        @NotNull RsStructLiteralField fieldToAdd,
        @NotNull List<RsStructLiteralField> existingFields,
        @NotNull List<RsFieldDecl> declaredFields
    ) {
        // If fieldToAdd is first in the original declaration, add it first
        if (!declaredFields.isEmpty()
            && fieldToAdd.getReferenceName() != null
            && fieldToAdd.getReferenceName().equals(declaredFields.get(0).getName())) {
            return existingFields.isEmpty() ? null : existingFields.get(0);
        }

        // If it was last, add last
        if (!declaredFields.isEmpty()
            && fieldToAdd.getReferenceName() != null
            && fieldToAdd.getReferenceName().equals(declaredFields.get(declaredFields.size() - 1).getName())) {
            return null;
        }

        int pos = -1;
        for (int i = 0; i < declaredFields.size(); i++) {
            if (declaredFields.get(i).getName() != null
                && declaredFields.get(i).getName().equals(fieldToAdd.getReferenceName())) {
                pos = i;
                break;
            }
        }
        if (pos == -1) {
            throw new IllegalStateException("Field not found in declared fields");
        }

        RsFieldDecl prev = declaredFields.get(pos - 1);
        RsFieldDecl next = declaredFields.get(pos + 1);

        int prevIdx = -1;
        int nextIdx = -1;
        for (int i = 0; i < existingFields.size(); i++) {
            if (existingFields.get(i).getReferenceName() != null) {
                if (existingFields.get(i).getReferenceName().equals(prev.getName())) {
                    prevIdx = i;
                }
                if (existingFields.get(i).getReferenceName().equals(next.getName())) {
                    nextIdx = i;
                }
            }
        }

        // Fit between two existing fields in the same order
        if (prevIdx != -1 && prevIdx + 1 == nextIdx) {
            return existingFields.get(nextIdx);
        }
        // We have next field, but the order is different
        if (nextIdx != -1) {
            return null;
        }

        if (prevIdx != -1) {
            int idx = prevIdx + 1;
            return idx < existingFields.size() ? existingFields.get(idx) : null;
        }

        return null;
    }

    @Nullable
    private RsStructLiteralField specializedCreateStructLiteralField(
        @NotNull RsFieldDecl fieldDecl,
        @NotNull Map<String, RsPatBinding> bindings
    ) {
        String fieldName = RsFieldDeclUtil.getEscapedName(fieldDecl);
        if (fieldName == null) return null;
        RsTypeReference typeRef = fieldDecl.getTypeReference();
        if (typeRef == null) return null;
        Ty fieldType = ExtensionsUtil.getNormType(typeRef);
        RsExpr fieldLiteral = buildFor(fieldType, bindings);
        return myPsiFactory.createStructLiteralField(fieldName, fieldLiteral);
    }
}
