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
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RsConvertToNamedFieldsProcessor extends BaseRefactoringProcessor {
    @Nonnull
    private final RsFieldsOwner myElement;
    private final boolean myConvertUsages;
    @Nonnull
    private final RsPsiFactory myRsPsiFactory;
    @Nonnull
    private final List<RsTupleFieldDecl> myTypes;
    @Nonnull
    private List<String> myNewNames;

    public RsConvertToNamedFieldsProcessor(
        @Nonnull Project project,
        @Nonnull RsFieldsOwner element,
        boolean convertUsages
    ) {
        super(project);
        myElement = element;
        myConvertUsages = convertUsages;
        myRsPsiFactory = new RsPsiFactory(project);
        myTypes = element.getTupleFields().getTupleFieldDeclList();
        myNewNames = new ArrayList<>();
        for (int i = 0; i <= myTypes.size(); i++) {
            myNewNames.add("_" + i);
        }
    }

    public RsConvertToNamedFieldsProcessor(
        @Nonnull Project project,
        @Nonnull RsFieldsOwner element,
        boolean convertUsages,
        @Nonnull List<String> newNames
    ) {
        this(project, element, convertUsages);
        myNewNames = newNames;
    }

    @Nonnull
    @Override
    protected UsageInfo[] findUsages() {
        if (!myConvertUsages) return new UsageInfo[0];
        List<UsageInfo> usages = new ArrayList<>();
        for (PsiReference ref : RsFieldsOwnerUtil.searchReferencesWithSelf(myElement)) {
            usages.add(new MyUsageInfo(ref, null));
        }
        for (int index = 0; index < myTypes.size(); index++) {
            ProgressManager.checkCanceled();
            RsTupleFieldDecl decl = myTypes.get(index);
            for (PsiReference ref : ReferencesSearch.search(decl)) {
                usages.add(new MyUsageInfo(ref, myNewNames.get(index)));
            }
        }
        return usages.toArray(new UsageInfo[0]);
    }

    private static class MyUsageInfo extends UsageInfo {
        @org.jetbrains.annotations.Nullable
        private final String myFieldName;

        MyUsageInfo(@Nonnull PsiReference psiReference, @org.jetbrains.annotations.Nullable String fieldName) {
            super(psiReference);
            myFieldName = fieldName;
        }
    }

    @Override
    protected void performRefactoring(@Nonnull UsageInfo[] usages) {
        for (UsageInfo usage : usages) {
            PsiElement element = usage.getElement();
            if (element == null) continue;
            PsiElement usageParent = element.getParent();
            if (usageParent instanceof RsDotExpr || usageParent instanceof RsStructLiteralBody || usageParent instanceof RsPatField) {
                PsiElement nameElement = ((RsMandatoryReferenceElement) element).getReferenceNameElement();
                nameElement.replace(myRsPsiFactory.createIdentifier(((MyUsageInfo) usage).myFieldName));
            } else if (usageParent instanceof RsPatTupleStruct) {
                RsPatTupleStruct patTupleStruct = (RsPatTupleStruct) usageParent;
                List<RsPat> patList = patTupleStruct.getPatList();
                int firstPatRestIndex = Integer.MAX_VALUE;
                int lastPatRestIndex = -1;
                for (int index = 0; index < patList.size(); index++) {
                    if (patList.get(index) instanceof RsPatRest) {
                        firstPatRestIndex = Math.min(firstPatRestIndex, index);
                        lastPatRestIndex = Math.max(lastPatRestIndex, index);
                    }
                }
                StringBuilder text = new StringBuilder();
                text.append("let ").append(patTupleStruct.getPath().getText()).append("{");
                boolean first = true;
                for (int index = 0; index < patList.size(); index++) {
                    if (index >= firstPatRestIndex && index <= firstPatRestIndex) continue;
                    RsPat pat = patList.get(index);
                    int fieldNumber;
                    if (index < firstPatRestIndex) {
                        fieldNumber = index;
                    } else if (index > lastPatRestIndex) {
                        fieldNumber = index + myTypes.size() - patList.size();
                    } else {
                        continue;
                    }
                    if (!first) text.append(", ");
                    text.append(myNewNames.get(fieldNumber)).append(":").append(pat.getText());
                    first = false;
                }
                if (lastPatRestIndex != -1) {
                    text.append(", ..");
                }
                text.append("} = 0;");
                PsiElement patternPsiElement = RsElementUtil.descendantOfTypeStrict(
                    myRsPsiFactory.createStatement(text.toString()), RsPatStruct.class);
                patTupleStruct.replace(patternPsiElement);
            } else {
                RsCallExpr callExpr = consulo.language.psi.util.PsiTreeUtil.getParentOfType(usageParent, RsCallExpr.class, false, RsValueArgumentList.class);
                if (callExpr == null) continue;
                RsExpr callExprExpr = callExpr.getExpr();
                if (callExprExpr instanceof RsPathExpr && ((RsPathExpr) callExprExpr).getPath() != element) continue;
                List<RsExpr> values = callExpr.getValueArgumentList().getExprList();
                StringBuilder text = new StringBuilder();
                text.append("let a = ").append(callExpr.getExpr().getText());
                text.append("{");
                for (int i = 0; i < values.size(); i++) {
                    if (i > 0) text.append(",\n");
                    text.append(myNewNames.get(i)).append(":").append(values.get(i).getText());
                }
                text.append("};");
                PsiElement structCreationElement = RsElementUtil.descendantOfTypeStrict(
                    myRsPsiFactory.createStatement(text.toString()), RsStructLiteral.class);
                callExpr.replace(structCreationElement);
            }
        }

        boolean isStruct = myElement instanceof RsStructItem;
        String separator = isStruct ? ",\n" : ", ";
        String end = isStruct && !myTypes.isEmpty() ? ",\n}" : "}";

        StringBuilder fields = new StringBuilder("{");
        for (int i = 0; i < myTypes.size(); i++) {
            if (i > 0) fields.append(separator);
            RsTupleFieldDecl tupleField = myTypes.get(i);
            String prefix = tupleField.getText().substring(0, tupleField.getTypeReference().getStartOffsetInParent());
            fields.append(prefix).append(myNewNames.get(i)).append(": ").append(tupleField.getTypeReference().getText());
        }
        fields.append(end);

        RsStructItem newFieldsElement = myRsPsiFactory.createStruct("struct A" + fields);

        RsTupleFields tupleFields = myElement.getTupleFields();
        if (tupleFields == null) return;
        RsBlockFields blockFields = newFieldsElement.getBlockFields();
        if (blockFields == null) return;

        RsWhereClause whereClause = myElement instanceof RsStructItem ? ((RsStructItem) myElement).getWhereClause() : null;
        if (whereClause == null) {
            tupleFields.replace(blockFields);
        } else {
            PsiElement nextSibling = whereClause.getNextSibling();
            if (nextSibling instanceof PsiWhiteSpace) nextSibling.delete();
            myElement.addAfter(blockFields, whereClause);
            tupleFields.delete();
            if (whereClause.getText().contains("\n")) {
                myElement.addAfter(myRsPsiFactory.createNewline(), whereClause);
            }
        }
        if (myElement instanceof RsStructItem) {
            PsiElement semicolon = ((RsStructItem) myElement).getSemicolon();
            if (semicolon != null) semicolon.delete();
        }
    }

    @Nonnull
    @Override
    protected consulo.localize.LocalizeValue getCommandName() {
        String name = myElement.getName() != null ? myElement.getName() : "";
        return consulo.localize.LocalizeValue.of(RsBundle.message("command.name.converting.to.named.fields", name));
    }

    @Nonnull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
        return new BaseUsageViewDescriptor(myElement);
    }

    @Nonnull
    @Override
    protected String getRefactoringId() {
        return "refactoring.convertToNamedFields";
    }
}
