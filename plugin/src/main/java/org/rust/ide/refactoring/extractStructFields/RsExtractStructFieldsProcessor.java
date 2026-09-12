/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractStructFields;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.editor.refactoring.BaseRefactoringProcessor;
import consulo.usage.BaseUsageViewDescriptor;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.presentation.TypeRendering;
import org.rust.ide.refactoring.ExtractSubsetUtils;
import org.rust.ide.refactoring.RsInPlaceVariableIntroducer;
import org.rust.ide.refactoring.RsNameSuggestions;
import org.rust.ide.refactoring.generate.StructMember;
import org.rust.ide.utils.GenericConstraints;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.Ty;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;
import org.rust.ide.inspections.lints.RsNamingInspection;
import org.rust.lang.core.psi.ext.RsFieldLookupUtil;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import consulo.localize.LocalizeValue;

public class RsExtractStructFieldsProcessor extends BaseRefactoringProcessor {
    private static final Set<String> TRANSITIVE_ATTRIBUTES = new HashSet<>(Arrays.asList("derive", "repr"));

    @Nonnull
    private final Editor myEditor;
    @Nonnull
    private final RsExtractStructFieldsContext myCtx;

    public RsExtractStructFieldsProcessor(
        @Nonnull Project project,
        @Nonnull Editor editor,
        @Nonnull RsExtractStructFieldsContext ctx
    ) {
        super(project);
        myEditor = editor;
        myCtx = ctx;
    }

    @Nonnull
    @Override
    protected UsageInfo[] findUsages() {
        List<UsageInfo> result = new ArrayList<>();

        ReferencesSearch.search(myCtx.getStruct()).forEach(ref -> {
            PsiElement element = ref.getElement();
            PsiElement parent = element.getParent();
            if (parent instanceof RsStructLiteral) {
                result.add(new LiteralUsage((RsStructLiteral) parent));
            } else if (parent instanceof RsPatStruct) {
                result.add(new PatUsage((RsPatStruct) parent));
            }
        });

        for (StructMember member : myCtx.getFields()) {
            ReferencesSearch.search(member.getField()).forEach(fieldUsage -> {
                PsiElement element = fieldUsage.getElement();
                if (element instanceof RsFieldLookup) {
                    result.add(new FieldUsage((RsFieldLookup) element));
                }
            });
        }

        return result.toArray(UsageInfo.EMPTY_ARRAY);
    }

    @Override
    protected void performRefactoring(@Nonnull UsageInfo[] usages) {
        RsStructItem struct = myCtx.getStruct();
        Project project = struct.getProject();
        RsPsiFactory factory = new RsPsiFactory(project);

        RsStructItem newStruct = createStruct(factory, myCtx);
        RsStructItem inserted = (RsStructItem) struct.getParent().addBefore(newStruct, struct);
        Ty type = inserted.getDeclaredType();

        RsNamedFieldDecl newField = replaceFields(factory, myCtx.getName(), struct, myCtx.getFields(), type);
        if (newField == null) return;
        String newFieldName = newField.getName();
        if (newFieldName == null) return;

        Map<String, RsFieldDecl> fieldMap = new LinkedHashMap<>();
        for (StructMember member : myCtx.getFields()) {
            if (member.getField() instanceof RsNamedFieldDecl) {
                RsNamedFieldDecl namedField = (RsNamedFieldDecl) member.getField();
                String name = namedField.getName();
                if (name != null) {
                    fieldMap.put(name, namedField);
                }
            }
        }

        List<RsReferenceElement> occurrences = new ArrayList<>();
        for (UsageInfo usage : usages) {
            RsReferenceElement occurrence = replaceUsage(factory, usage, newFieldName, myCtx.getName(), fieldMap);
            if (occurrence != null) {
                occurrences.add(occurrence);
            }
        }

        for (RsReferenceElement occurrence : occurrences) {
            if (occurrence.getReference() != null && occurrence.getReference().resolve() == null) {
                RsImportHelper.importElement(occurrence, inserted);
            }
        }

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(myEditor.getDocument());

        myEditor.getCaretModel().moveToOffset(newField.getIdentifier().getTextOffset());
        new RsInPlaceVariableIntroducer(newField, myEditor, project, RsBundle.message("command.name.choose.field.name"))
            .performInplaceRefactoring(null);
    }

    @Nonnull
    @Override
    protected consulo.localize.LocalizeValue getCommandName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("action.Rust.RsExtractStructFields.command.name"));
    }

    @Nonnull
    @Override
    protected UsageViewDescriptor createUsageViewDescriptor(@Nonnull UsageInfo[] usages) {
        return new BaseUsageViewDescriptor(myCtx.getStruct());
    }

    @Nullable
    @Override
    protected String getRefactoringId() {
        return "refactoring.extractStructFields";
    }

    @Nullable
    private static RsNamedFieldDecl replaceFields(
        @Nonnull RsPsiFactory factory,
        @Nonnull String name,
        @Nonnull RsStructItem struct,
        @Nonnull List<StructMember> replacedFields,
        @Nonnull Ty type
    ) {
        RsVis visibility = getBroadestVisibility(factory, replacedFields);
        String visibilityText = visibility != null ? visibility.getText() + " " : "";
        String fieldName = generateName(struct, name);
        String text = visibilityText + fieldName + ": " + TypeRendering.renderInsertionSafe(type, true, false);

        RsNamedFieldDecl newField = factory.createStructNamedField(text);
        PsiElement firstField = replacedFields.get(0).getField();
        RsNamedFieldDecl insertedField = (RsNamedFieldDecl) firstField.getParent().addBefore(newField, firstField);
        insertedField.getParent().addAfter(factory.createComma(), insertedField);

        for (StructMember member : replacedFields) {
            PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(member.getField());
        }
        return insertedField;
    }

    @Nonnull
    private static String generateName(@Nonnull RsStructItem struct, @Nonnull String structName) {
        String fieldName = RsNamingInspection.toSnakeCase(structName, false);
        String currentName = fieldName;
        int index = 0;
        Set<String> fieldNames = new HashSet<>();
        if (struct.getBlockFields() != null) {
            for (RsNamedFieldDecl field : struct.getBlockFields().getNamedFieldDeclList()) {
                if (field.getName() != null) {
                    fieldNames.add(field.getName());
                }
            }
        }
        while (fieldNames.contains(currentName)) {
            currentName = fieldName + index;
            index++;
        }
        return currentName;
    }

    @Nonnull
    private static RsStructItem createStruct(@Nonnull RsPsiFactory factory, @Nonnull RsExtractStructFieldsContext ctx) {
        RsStructItem struct = ctx.getStruct();
        RsVis vis = struct.getVis();
        String formattedVis = vis == null ? "" : vis.getText() + " ";
        String fieldsText = ctx.getFields().stream()
            .map(m -> m.getField().getText())
            .collect(Collectors.joining(",\n"));
        List<RsOuterAttr> attributes = ExtractSubsetUtils.findTransitiveAttributes(struct, TRANSITIVE_ATTRIBUTES);

        List<RsTypeReference> fieldTypeReferences = ctx.getFields().stream()
            .map(m -> m.getField().getTypeReference())
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        GenericConstraints genericConstraints = GenericConstraints.create(struct)
            .filterByTypeReferences(fieldTypeReferences);

        String typeParameters = genericConstraints.buildTypeParameters();
        String whereClause = genericConstraints.buildWhereClause();

        String text = formattedVis + "struct " + ctx.getName() + typeParameters + whereClause + " {\n" + fieldsText + "\n}";
        String attributesText = attributes.isEmpty() ? "" :
            attributes.stream().map(PsiElement::getText).collect(Collectors.joining("\n", "", "\n"));
        return factory.createStruct(attributesText + text);
    }

    @Nullable
    private static RsVis getBroadestVisibility(@Nonnull RsPsiFactory factory, @Nonnull List<StructMember> replacedFields) {
        List<RsVis> visibilities = replacedFields.stream()
            .map(m -> m.getField().getVis())
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        for (RsVis vis : visibilities) {
            if (RsVisibilityUtil.getVisibility(vis) == RsVisibility.Public.INSTANCE) {
                return vis;
            }
        }

        for (RsVis vis : visibilities) {
            RsVisibility visibility = RsVisibilityUtil.getVisibility(vis);
            if (visibility instanceof RsVisibility.Restricted) {
                RsMod inMod = ((RsVisibility.Restricted) visibility).getInMod();
                if (RsModUtil.isCrateRoot(inMod)) {
                    return vis;
                }
            }
        }

        RsMod broadest = null;
        for (RsVis vis : visibilities) {
            RsVisibility visibility = RsVisibilityUtil.getVisibility(vis);
            if (visibility instanceof RsVisibility.Restricted) {
                RsMod module = ((RsVisibility.Restricted) visibility).getInMod();
                if (broadest == null) {
                    broadest = module;
                } else {
                    RsMod common = PsiElementUtil.commonParentMod(broadest, module);
                    if (common == null) {
                        return factory.createVis("pub(crate)");
                    }
                    broadest = common;
                }
            }
        }
        if (broadest != null) {
            return factory.createVis("pub(in " + ((RsQualifiedNamedElement) broadest).qualifiedName() + ")");
        }
        return null;
    }

    @Nullable
    private static RsReferenceElement replaceUsage(
        @Nonnull RsPsiFactory factory,
        @Nonnull UsageInfo usage,
        @Nonnull String fieldName,
        @Nonnull String structName,
        @Nonnull Map<String, RsFieldDecl> fields
    ) {
        if (usage instanceof LiteralUsage) {
            return replaceLiteralUsage(factory, ((LiteralUsage) usage).myLiteral, fieldName, structName, fields);
        } else if (usage instanceof PatUsage) {
            return replacePatUsage(factory, ((PatUsage) usage).myPat, fieldName, structName, fields);
        } else if (usage instanceof FieldUsage) {
            return replaceFieldUsage(factory, ((FieldUsage) usage).myFieldLookup, fieldName);
        }
        return null;
    }

    @Nonnull
    private static RsReferenceElement replaceLiteralUsage(
        @Nonnull RsPsiFactory factory,
        @Nonnull RsStructLiteral literal,
        @Nonnull String fieldName,
        @Nonnull String structName,
        @Nonnull Map<String, RsFieldDecl> fields
    ) {
        Set<String> foundFields = new HashSet<>();
        List<RsStructLiteralField> fieldExprs = new ArrayList<>();
        RsStructLiteralBody body = literal.getStructLiteralBody();
        for (RsStructLiteralField field : body.getStructLiteralFieldList()) {
            String name = field.getIdentifier() != null ? field.getIdentifier().getText() : null;
            if (name != null && fields.containsKey(name)) {
                fieldExprs.add(field);
                foundFields.add(name);
            }
        }
        if (fieldExprs.size() < fields.size()) {
            RsExpr expr = body.getExpr();
            PsiElement dotdot = body.getDotdot();
            if (dotdot != null && expr != null) {
                RsExpr extractedExpr = extractDotDotExpr(factory, literal, expr);
                Set<String> missingKeys = new HashSet<>(fields.keySet());
                missingKeys.removeAll(foundFields);
                for (String key : missingKeys) {
                    RsStructLiteralField literalField = factory.createStructLiteralField(key, extractedExpr.getText() + "." + fieldName + "." + key);
                    fieldExprs.add(literalField);
                }
                expr.replace(extractedExpr);
            }
        }

        String fieldsStr = fieldExprs.stream().map(PsiElement::getText).collect(Collectors.joining(", "));
        RsStructLiteral newLiteral = factory.createStructLiteral(structName, "{ " + fieldsStr + " }");
        RsStructLiteralField newField = factory.createStructLiteralField(fieldName, newLiteral);
        PsiElement anchor = null;
        for (RsStructLiteralField fe : fieldExprs) {
            if (fe.isPhysical()) {
                anchor = fe;
                break;
            }
        }
        if (anchor == null) {
            anchor = body.getDotdot();
        }
        if (anchor == null) {
            anchor = body.getExpr();
        }
        if (anchor == null) {
            anchor = body.getRbrace();
        }
        RsStructLiteralField inserted = (RsStructLiteralField) body.addBefore(newField, anchor);

        for (RsStructLiteralField fe : fieldExprs) {
            PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(fe);
        }

        PsiElement nextSibling = PsiElementUtil.getNextNonCommentSibling(inserted);
        if (nextSibling == null || nextSibling.getNode().getElementType() != RsElementTypes.RBRACE) {
            inserted.getParent().addAfter(factory.createComma(), inserted);
        }

        return ((RsStructLiteral) inserted.getExpr()).getPath();
    }

    @Nonnull
    private static RsReferenceElement replacePatUsage(
        @Nonnull RsPsiFactory factory,
        @Nonnull RsPatStruct pat,
        @Nonnull String fieldName,
        @Nonnull String structName,
        @Nonnull Map<String, RsFieldDecl> fields
    ) {
        List<RsPatField> fieldPats = new ArrayList<>();
        for (RsPatField field : pat.getPatFieldList()) {
            RsPatFieldFull patFieldFull = field.getPatFieldFull();
            RsPatBinding patBinding = field.getPatBinding();
            String name;
            if (patFieldFull != null) {
                name = patFieldFull.getIdentifier() != null ? patFieldFull.getIdentifier().getText() : "";
            } else if (patBinding != null) {
                name = patBinding.getIdentifier().getText();
            } else {
                continue;
            }
            if (fields.containsKey(name)) {
                fieldPats.add(field);
            }
        }
        RsPatRest rest = fieldPats.size() < fields.size() ? factory.createPatRest() : null;

        RsPatStruct newStructPat = factory.createPatStruct(structName, fieldPats, rest);
        RsPatFieldFull newFieldPat = factory.createPatFieldFull(fieldName, newStructPat.getText());
        PsiElement anchor = !fieldPats.isEmpty() ? fieldPats.get(0) : (pat.getPatRest() != null ? pat.getPatRest() : pat.getRbrace());
        RsPatFieldFull inserted = (RsPatFieldFull) pat.addBefore(newFieldPat, anchor);

        for (RsPatField fp : fieldPats) {
            PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(fp);
        }

        PsiElement nextSibling = PsiElementUtil.getNextNonCommentSibling(inserted);
        if (nextSibling == null || nextSibling.getNode().getElementType() != RsElementTypes.RBRACE) {
            inserted.getParent().addAfter(factory.createComma(), inserted);
        }

        return ((RsPatStruct) inserted.getPat()).getPath();
    }

    @Nullable
    private static RsReferenceElement replaceFieldUsage(
        @Nonnull RsPsiFactory factory,
        @Nonnull RsFieldLookup fieldLookup,
        @Nonnull String fieldName
    ) {
        RsDotExpr dotExpr = RsFieldLookupUtil.getParentDotExpr(fieldLookup);
        RsExpr newDotExpr = factory.createExpression(dotExpr.getExpr().getText() + "." + fieldName);
        dotExpr.getExpr().replace(newDotExpr);
        return null;
    }

    @Nonnull
    private static RsExpr extractDotDotExpr(
        @Nonnull RsPsiFactory factory,
        @Nonnull RsStructLiteral literal,
        @Nonnull RsExpr expr
    ) {
        if (expr instanceof RsPathExpr) return expr;

        String name = RsNameSuggestions.suggestedNames(expr).getDefault();
        RsLetDecl variable = factory.createLetDeclaration(name, expr);

        PsiElement anchor = PsiTreeUtil.getParentOfType(literal, RsStmt.class);
        if (anchor == null) {
            anchor = literal;
        }
        anchor.getParent().addBefore(variable, anchor);
        return factory.createExpression(name);
    }

    private static class LiteralUsage extends UsageInfo {
        @Nonnull
        final RsStructLiteral myLiteral;

        LiteralUsage(@Nonnull RsStructLiteral literal) {
            super(literal);
            myLiteral = literal;
        }
    }

    private static class PatUsage extends UsageInfo {
        @Nonnull
        final RsPatStruct myPat;

        PatUsage(@Nonnull RsPatStruct pat) {
            super(pat);
            myPat = pat;
        }
    }

    private static class FieldUsage extends UsageInfo {
        @Nonnull
        final RsFieldLookup myFieldLookup;

        FieldUsage(@Nonnull RsFieldLookup fieldLookup) {
            super(fieldLookup);
            myFieldLookup = fieldLookup;
        }
    }
}
