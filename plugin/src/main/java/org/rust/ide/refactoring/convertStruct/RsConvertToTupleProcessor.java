/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.convertStruct;

import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.usage.BaseUsageViewDescriptor;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewDescriptor;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.*;

import java.util.*;
import java.util.stream.Collectors;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.impl.*;

public class RsConvertToTupleProcessor extends BaseRefactoringProcessor {
    @Nonnull
    private final RsFieldsOwner myElement;
    private final boolean myConvertUsages;
    @Nonnull
    private final RsPsiFactory myRsPsiFactory;
    @Nonnull
    private final List<RsNamedFieldDecl> myFieldDeclList;

    public RsConvertToTupleProcessor(
        @Nonnull Project project,
        @Nonnull RsFieldsOwner element,
        boolean convertUsages
    ) {
        super(project);
        myElement = element;
        myConvertUsages = convertUsages;
        myRsPsiFactory = new RsPsiFactory(project);
        myFieldDeclList = element.getBlockFields().getNamedFieldDeclList();
    }

    @Nonnull
    @Override
    protected UsageInfo[] findUsages() {
        if (!myConvertUsages) return new UsageInfo[0];
        List<UsageInfo> usages = new ArrayList<>();
        for (PsiReference ref : RsFieldsOwnerUtil.searchReferencesWithSelf(myElement)) {
            usages.add(new UsageInfo(ref));
        }
        for (int index = 0; index < myFieldDeclList.size(); index++) {
            ProgressManager.checkCanceled();
            RsNamedFieldDecl decl = myFieldDeclList.get(index);
            for (PsiReference ref : ReferencesSearch.search(decl)) {
                if (ref.getElement().getParent() instanceof RsDotExpr) {
                    usages.add(new MyUsageInfo(ref, index));
                }
            }
        }
        return usages.toArray(new UsageInfo[0]);
    }

    private static class MyUsageInfo extends UsageInfo {
        private final int myPosition;

        MyUsageInfo(@Nonnull PsiReference psiReference, int position) {
            super(psiReference);
            myPosition = position;
        }
    }

    @Override
    protected void performRefactoring(@Nonnull UsageInfo[] usages) {
        for (UsageInfo usage : usages) {
            PsiElement element = usage.getElement();
            if (element == null) continue;
            PsiElement usageParent = element.getParent();
            if (usageParent instanceof RsDotExpr) {
                int pos = ((MyUsageInfo) usage).myPosition;
                PsiElement replacement = RsElementUtil.descendantOfTypeStrict(
                    myRsPsiFactory.createExpression("a." + pos), RsFieldLookup.class);
                element.replace(replacement);
            } else if (usageParent instanceof RsPatStruct) {
                RsPatStruct patStruct = (RsPatStruct) usageParent;
                Map<String, String> patternFieldMap = new LinkedHashMap<>();
                for (RsPatField field : patStruct.getPatFieldList()) {
                    RsPatFieldKind kind = RsPatFieldUtil.getKind(field);
                    if (kind instanceof RsPatFieldKind.Full) {
                        patternFieldMap.put(((RsPatFieldKind.Full) kind).getFieldName(), ((RsPatFieldKind.Full) kind).getPat().getText());
                    } else if (kind instanceof RsPatFieldKind.Shorthand) {
                        patternFieldMap.put(((RsPatFieldKind.Shorthand) kind).getFieldName(), ((RsPatFieldKind.Shorthand) kind).getBinding().getText());
                    }
                }
                StringBuilder text = new StringBuilder("let ");
                text.append(patStruct.getPath().getText());
                text.append("(");
                for (int i = 0; i < myFieldDeclList.size(); i++) {
                    if (i > 0) text.append(", ");
                    String val = patternFieldMap.get(myFieldDeclList.get(i).getIdentifier().getText());
                    text.append(val != null ? val : "_ ");
                }
                text.append(") = 0;");
                PsiElement patternPsiElement = RsElementUtil.descendantOfTypeStrict(
                    myRsPsiFactory.createStatement(text.toString()), RsPatTupleStruct.class);
                usageParent.replace(patternPsiElement);
            } else if (usageParent instanceof RsStructLiteral) {
                RsStructLiteral structLiteral = (RsStructLiteral) usageParent;
                if (structLiteral.getStructLiteralBody().getDotdot() != null) {
                    StringBuilder text = new StringBuilder("let a = ");
                    text.append(structLiteral.getPath().getText()).append("{");
                    List<RsStructLiteralField> fields = structLiteral.getStructLiteralBody().getStructLiteralFieldList();
                    for (int i = 0; i < fields.size(); i++) {
                        if (i > 0) text.append(",");
                        RsStructLiteralField f = fields.get(i);
                        int idx = -1;
                        for (int j = 0; j < myFieldDeclList.size(); j++) {
                            if (myFieldDeclList.get(j).getIdentifier().textMatches(f.getIdentifier())) {
                                idx = j;
                                break;
                            }
                        }
                        String val = f.getExpr() != null ? f.getExpr().getText() : f.getIdentifier().getText();
                        text.append(idx).append(":").append(val);
                    }
                    text.append(", ..").append(structLiteral.getStructLiteralBody().getExpr().getText()).append("};");
                    PsiElement newElement = RsElementUtil.descendantOfTypeStrict(
                        myRsPsiFactory.createStatement(text.toString()), RsStructLiteral.class);
                    usageParent.replace(newElement);
                } else {
                    Map<String, String> valuesMap = new LinkedHashMap<>();
                    for (RsStructLiteralField f : structLiteral.getStructLiteralBody().getStructLiteralFieldList()) {
                        String val = f.getExpr() != null ? f.getExpr().getText() : f.getIdentifier().getText();
                        valuesMap.put(f.getIdentifier().getText(), val);
                    }
                    StringBuilder text = new StringBuilder("let a = ");
                    text.append(structLiteral.getPath().getText()).append("(");
                    for (int i = 0; i < myFieldDeclList.size(); i++) {
                        if (i > 0) text.append(", ");
                        String val = valuesMap.get(myFieldDeclList.get(i).getIdentifier().getText());
                        text.append(val != null ? val : "_ ");
                    }
                    text.append(");");
                    PsiElement newElement = RsElementUtil.descendantOfTypeStrict(
                        myRsPsiFactory.createStatement(text.toString()), RsCallExpr.class);
                    usageParent.replace(newElement);
                }
            }
        }

        StringBuilder types = new StringBuilder("(");
        for (int i = 0; i < myFieldDeclList.size(); i++) {
            if (i > 0) types.append(",");
            RsNamedFieldDecl decl = myFieldDeclList.get(i);
            String prefix = decl.getText().substring(0, decl.getIdentifier().getStartOffsetInParent());
            types.append(prefix);
            if (decl.getTypeReference() != null) types.append(decl.getTypeReference().getText());
        }
        types.append(")");

        RsStructItem newTuplePsiElement = myRsPsiFactory.createStruct("struct A" + types + ";");
        RsBlockFields blockFields = myElement.getBlockFields();
        if (blockFields == null) return;
        RsTupleFields tupleFields = newTuplePsiElement.getTupleFields();
        if (tupleFields == null) return;

        RsWhereClause whereClause = myElement instanceof RsStructItem ? ((RsStructItem) myElement).getWhereClause() : null;
        if (whereClause == null) {
            blockFields.replace(tupleFields);
        } else {
            PsiElement prev = RsElementUtil.getPrevNonWhitespaceSibling(whereClause);
            myElement.addAfter(tupleFields, prev);
            PsiElement prevSib = blockFields.getPrevSibling();
            if (prevSib instanceof PsiWhiteSpace) prevSib.delete();
            blockFields.delete();
            PsiElement lastChild = whereClause.getLastChild();
            if (lastChild != null && lastChild.getNode().getElementType() == RsElementTypes.COMMA) {
                lastChild.delete();
            }
        }
        if (myElement instanceof RsStructItem) {
            myElement.addAfter(myRsPsiFactory.createSemicolon(), myElement.getLastChild());
        }
    }

    @Nonnull
    @Override
    protected consulo.localize.LocalizeValue getCommandName() {
        String name = myElement.getName() != null ? myElement.getName() : "";
        return consulo.localize.LocalizeValue.of(RsBundle.message("command.name.converting.to.tuple", name));
    }

    @Nonnull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
        return new BaseUsageViewDescriptor(myElement);
    }

    @Nonnull
    @Override
    protected String getRefactoringId() {
        return "refactoring.convertToTuple";
    }
}
