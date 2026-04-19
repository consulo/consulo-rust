/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractEnumVariant;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.usageView.BaseUsageViewDescriptor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.refactoring.ExtractSubsetUtils;
import org.rust.ide.refactoring.RsInPlaceVariableIntroducer;
import org.rust.ide.utils.GenericConstraints;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsEnumVariantUtil;

public class RsExtractEnumVariantProcessor extends BaseRefactoringProcessor {
    @NotNull
    private final Editor myEditor;
    @NotNull
    private final RsEnumVariant myCtx;

    public static final Set<String> TRANSITIVE_ATTRIBUTES = new HashSet<>(Arrays.asList("derive", "repr"));

    public RsExtractEnumVariantProcessor(@NotNull Project project, @NotNull Editor editor, @NotNull RsEnumVariant ctx) {
        super(project);
        myEditor = editor;
        myCtx = ctx;
    }

    @NotNull
    @Override
    protected UsageInfo[] findUsages() {
        return ReferencesSearch.search(myCtx).findAll().stream()
            .map(UsageInfo::new)
            .toArray(UsageInfo[]::new);
    }

    @Override
    protected void performRefactoring(@NotNull UsageInfo[] usages) {
        Project project = myCtx.getProject();
        RsPsiFactory factory = new RsPsiFactory(project);

        String name = myCtx.getName();
        if (name == null) return;

        RsTupleFields tupleFields = myCtx.getTupleFields();
        RsBlockFields blockFields = myCtx.getBlockFields();
        boolean isTuple = tupleFields != null;

        PsiElement toBeReplaced = isTuple ? tupleFields : blockFields;
        if (toBeReplaced == null) return;

        List<RsTypeReference> typeReferences;
        if (isTuple) {
            typeReferences = tupleFields.getTupleFieldDeclList().stream()
                .map(RsTupleFieldDecl::getTypeReference)
                .collect(Collectors.toList());
        } else {
            typeReferences = blockFields.getNamedFieldDeclList().stream()
                .map(RsNamedFieldDecl::getTypeReference)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        List<RsReferenceElement> occurrences = new ArrayList<>();
        for (UsageInfo usage : usages) {
            PsiElement reference = usage.getElement();
            if (reference == null) continue;
            PsiElement occurrence = replaceUsage(reference, name, factory, isTuple, toBeReplaced);
            if (occurrence instanceof RsReferenceElement) {
                occurrences.add((RsReferenceElement) occurrence);
            }
        }

        RsEnumItem enumItem = RsEnumVariantUtil.getParentEnum(myCtx);
        GenericConstraints genericConstraints = GenericConstraints.create(myCtx)
            .filterByTypeReferences(typeReferences);

        String typeParametersText = genericConstraints.buildTypeParameters();
        String whereClause = genericConstraints.buildWhereClause();
        List<RsOuterAttr> attributes = ExtractSubsetUtils.findTransitiveAttributes(enumItem, TRANSITIVE_ATTRIBUTES);

        String vis = enumItem.getVis() != null ? enumItem.getVis().getText() : null;
        String formattedVis = vis == null ? "" : vis + " ";

        // Add pub to fields
        List<? extends RsFieldDecl> decls;
        if (isTuple) {
            decls = tupleFields.getTupleFieldDeclList();
        } else {
            decls = blockFields.getNamedFieldDeclList();
        }
        for (RsFieldDecl decl : decls) {
            if (decl.getVis() == null) {
                PsiElement pub = factory.createPub();
                if (decl instanceof RsNamedFieldDecl) {
                    decl.addBefore(pub, ((RsNamedFieldDecl) decl).getIdentifier());
                } else if (decl instanceof RsTupleFieldDecl) {
                    decl.addBefore(pub, ((RsTupleFieldDecl) decl).getTypeReference());
                }
            }
        }

        String fieldsText = toBeReplaced.getText();
        String text;
        if (isTuple) {
            text = formattedVis + "struct " + name + typeParametersText + fieldsText + whereClause + ";";
        } else {
            text = formattedVis + "struct " + name + typeParametersText + whereClause + fieldsText;
        }
        String attributesText = attributes.stream().map(PsiElement::getText).collect(Collectors.joining("\n"));
        String textWithAttributes = attributesText + "\n" + text;
        RsStructItem struct = factory.createStruct(textWithAttributes);
        RsStructItem inserted = (RsStructItem) enumItem.getParent().addBefore(struct, enumItem);

        for (RsReferenceElement occurrence : occurrences) {
            if (occurrence.getReference() == null || occurrence.getReference().resolve() == null) {
                RsImportHelper.importElement(occurrence, inserted);
            }
        }

        RsPsiFactory.TupleField tupleField = new RsPsiFactory.TupleField(inserted.getDeclaredType(), false);
        RsTupleFields newFields = factory.createTupleFields(Collections.singletonList(tupleField));
        RsTupleFields replaced = (RsTupleFields) toBeReplaced.replace(newFields);
        RsPath pathInReplaced = PsiTreeUtil.findChildOfType(replaced, RsPath.class);
        if (pathInReplaced != null) {
            occurrences.add(pathInReplaced);
        }

        List<PsiElement> additionalElementsToRename = occurrences.stream()
            .filter(o -> o.getReference() == null || o.getReference().resolve() != inserted)
            .collect(Collectors.toList());

        offerStructRename(project, myEditor, inserted, additionalElementsToRename);
    }

    private PsiElement replaceUsage(
        @NotNull PsiElement element,
        @NotNull String name,
        @NotNull RsPsiFactory factory,
        boolean isTuple,
        @NotNull PsiElement toBeReplaced
    ) {
        if (isTuple) {
            return replaceTupleUsage(element, name, factory, (RsTupleFields) toBeReplaced);
        } else {
            return replaceStructUsage(element, name, factory);
        }
    }

    private PsiElement replaceTupleUsage(
        @NotNull PsiElement element,
        @NotNull String name,
        @NotNull RsPsiFactory factory,
        @NotNull RsTupleFields fields
    ) {
        PsiElement pathExpr = element.getParent();
        if (pathExpr instanceof RsPathExpr) {
            PsiElement parent = pathExpr.getParent();
            if (parent instanceof RsCallExpr) {
                // constructor call
                RsCallExpr callExpr = (RsCallExpr) parent;
                String argumentsText = callExpr.getValueArgumentList().getText();
                argumentsText = argumentsText.substring(1, argumentsText.length() - 1);
                RsCallExpr call = factory.createFunctionCall(name, argumentsText);
                RsCallExpr binding = factory.createFunctionCall(callExpr.getExpr().getText(), Collections.singletonList(call));
                RsCallExpr replaced = (RsCallExpr) callExpr.replace(binding);
                RsExpr firstArg = replaced.getValueArgumentList().getExprList().isEmpty() ? null : replaced.getValueArgumentList().getExprList().get(0);
                if (firstArg == null) return null;
                return PsiTreeUtil.findChildOfType(firstArg, RsReferenceElement.class);
            } else {
                // constructor reference
                int argCount = fields.getTupleFieldDeclList().size();
                List<String> args = new ArrayList<>();
                for (int i = 0; i < argCount; i++) args.add("p" + i);
                String argumentsText = String.join(",", args);
                RsCallExpr call = factory.createFunctionCall(name, argumentsText);
                RsCallExpr binding = factory.createFunctionCall(((RsPathExpr) pathExpr).getPath().getText(), Collections.singletonList(call));
                RsLambdaExpr lambda = (RsLambdaExpr) factory.createExpression("|" + argumentsText + "| " + binding.getText());
                RsLambdaExpr replaced = (RsLambdaExpr) pathExpr.replace(lambda);
                RsCallExpr replacedCall = (RsCallExpr) replaced.getExpr();
                RsExpr firstArg = replacedCall.getValueArgumentList().getExprList().isEmpty() ? null : replacedCall.getValueArgumentList().getExprList().get(0);
                if (firstArg == null) return null;
                return PsiTreeUtil.findChildOfType(firstArg, RsReferenceElement.class);
            }
        } else {
            RsPatTupleStruct patTupleStruct = PsiTreeUtil.getParentOfType(element, RsPatTupleStruct.class);
            if (patTupleStruct == null) return null;
            RsPatTupleStruct innerPat = factory.createPatTupleStruct(name, patTupleStruct.getPatList());
            RsPatTupleStruct bindingPat = factory.createPatTupleStruct(patTupleStruct.getPath().getText(), Collections.singletonList(innerPat));
            RsPatTupleStruct replacedPat = (RsPatTupleStruct) patTupleStruct.replace(bindingPat);
            RsPat firstPat = replacedPat.getPatList().isEmpty() ? null : replacedPat.getPatList().get(0);
            if (firstPat == null) return null;
            return PsiTreeUtil.findChildOfType(firstPat, RsReferenceElement.class);
        }
    }

    private PsiElement replaceStructUsage(
        @NotNull PsiElement element,
        @NotNull String name,
        @NotNull RsPsiFactory factory
    ) {
        PsiElement parent = PsiTreeUtil.getParentOfType(element, RsPatStruct.class, RsStructLiteral.class);
        if (parent instanceof RsPatStruct) {
            RsPatStruct patStruct = (RsPatStruct) parent;
            RsPath path = patStruct.getPath();
            RsPatStruct binding = factory.createPatStruct(name, patStruct.getPatFieldList(), patStruct.getPatRest());
            RsPatTupleStruct newPat = factory.createPatTupleStruct(path.getText(), Collections.singletonList(binding));
            RsPatTupleStruct replaced = (RsPatTupleStruct) patStruct.replace(newPat);
            RsPat firstPat = replaced.getPatList().isEmpty() ? null : replaced.getPatList().get(0);
            if (firstPat == null) return null;
            return PsiTreeUtil.findChildOfType(firstPat, RsReferenceElement.class);
        } else if (parent instanceof RsStructLiteral) {
            RsStructLiteral structLiteral = (RsStructLiteral) parent;
            RsStructLiteral literal = factory.createStructLiteral(name, structLiteral.getStructLiteralBody().getText());
            RsCallExpr binding = factory.createFunctionCall(structLiteral.getPath().getText(), Collections.singletonList(literal));
            RsCallExpr replaced = (RsCallExpr) structLiteral.replace(binding);
            RsExpr firstArg = replaced.getValueArgumentList().getExprList().isEmpty() ? null : replaced.getValueArgumentList().getExprList().get(0);
            if (firstArg == null) return null;
            return PsiTreeUtil.findChildOfType(firstArg, RsReferenceElement.class);
        }
        return null;
    }

    @NotNull
    @Override
    protected String getCommandName() {
        String name = myCtx.getName() != null ? myCtx.getName() : "";
        return RsBundle.message("command.name.extracting.variant", name);
    }

    @NotNull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo[] usages) {
        return new BaseUsageViewDescriptor(myCtx);
    }

    @NotNull
    @Override
    protected String getRefactoringId() {
        return "refactoring.extractEnumVariant";
    }

    private static void offerStructRename(
        @NotNull Project project,
        @NotNull Editor editor,
        @NotNull RsStructItem inserted,
        @NotNull List<PsiElement> additionalElementsToRename
    ) {
        PsiElement identifier = inserted.getIdentifier();
        if (identifier == null) return;
        com.intellij.openapi.util.TextRange range = identifier.getTextRange();
        if (range == null) return;
        editor.getCaretModel().moveToOffset(range.getStartOffset());
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
        new RsInPlaceVariableIntroducer(inserted, editor, project,
            RsBundle.message("command.name.choose.struct.name"), additionalElementsToRename)
            .performInplaceRefactoring(null);
    }
}
