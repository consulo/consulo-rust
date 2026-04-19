/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.createFromUsage;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.RsElementBaseIntentionAction;
import org.rust.ide.presentation.RsPsiRenderingUtil;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.ide.utils.imports.ImportBridge;
import org.rust.ide.utils.template.EditorExt;
import org.rust.ide.utils.template.RsTemplateBuilder;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.openapiext.PsiExtUtil;
import org.rust.openapiext.SmartPointerUtil;
import org.rust.lang.core.psi.ext.RsStructLiteralFieldUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

public class CreateStructIntention extends RsElementBaseIntentionAction<CreateStructIntention.Context> {
    @Override
    @NotNull
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.create.struct");
    }

    public static class Context {
        @NotNull
        public final String name;
        @NotNull
        public final RsStructLiteral structLiteral;
        @NotNull
        public final RsMod targetMod;
        @NotNull
        public final PsiInsertionPlace place;

        public Context(@NotNull String name, @NotNull RsStructLiteral structLiteral,
                        @NotNull RsMod targetMod, @NotNull PsiInsertionPlace place) {
            this.name = name;
            this.structLiteral = structLiteral;
            this.targetMod = targetMod;
            this.place = place;
        }
    }

    @Override
    @Nullable
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsPath path = RsElementUtil.contextStrict(element, RsPath.class);
        RsStructLiteral structLiteral = path != null ? RsElementUtil.contextStrict(path, RsStructLiteral.class) : null;
        if (structLiteral != null) {
            if (!structLiteral.getPath().equals(path)) return null;
            if (RsPathUtil.getResolveStatus(path) != PathResolveStatus.UNRESOLVED) return null;

            RsMod targetMod = CreateFromUsageUtils.getWritablePathMod(path);
            if (targetMod == null) return null;
            String name = path.getReferenceName();
            if (name == null) return null;
            PsiInsertionPlace place = PsiInsertionPlace.forItemInModBefore(targetMod, structLiteral);
            if (place == null) return null;

            setText(RsBundle.message("intention.name.create.struct", name));
            return new Context(name, structLiteral, targetMod, place);
        }
        return null;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        RsStructItem newStruct = buildStruct(project, ctx);
        if (newStruct == null) return;
        RsStructItem inserted = ctx.place.insert(newStruct);

        List<Ty> types = ctx.structLiteral.getStructLiteralBody().getStructLiteralFieldList().stream()
            .map(f -> f.getExpr() != null ? RsTypesUtil.getType(f.getExpr()) : null)
            .filter(t -> t != null)
            .collect(Collectors.toList());
        ImportBridge.importTypeReferencesFromTys(inserted, types);

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        List<RsNamedFieldDecl> fields = inserted.getBlockFields() != null
            ? inserted.getBlockFields().getNamedFieldDeclList()
            : java.util.Collections.emptyList();
        List<SmartPsiElementPointer<RsNamedFieldDecl>> fieldPointers = fields.stream()
            .map(SmartPointerUtil::createSmartPointer)
            .collect(Collectors.toList());

        if (EditorExt.canRunTemplateFor(editor, inserted) && !fieldPointers.isEmpty()) {
            List<RsInferType> unknownTypes = RsElementUtil.descendantsOfType(inserted, RsInferType.class);
            RsTemplateBuilder tpl = EditorExt.newTemplateBuilder(editor, inserted);

            // Replace unknown types
            for (RsInferType inferType : unknownTypes) {
                tpl.replaceElement(inferType, (String) null);
            }

            // Replace field names
            for (SmartPsiElementPointer<RsNamedFieldDecl> fieldPtr : fieldPointers) {
                RsNamedFieldDecl field = fieldPtr.getElement();
                if (field == null) continue;
                PsiElement identifier = field.getIdentifier();
                if (identifier == null) continue;
                RsTemplateBuilder.TemplateVariable variable = tpl.introduceVariable(identifier);
                List<RsStructLiteralField> litFields = ctx.structLiteral.getStructLiteralBody().getStructLiteralFieldList();
                for (RsStructLiteralField litField : litFields) {
                    if (litField.getIdentifier() != null && litField.getIdentifier().getText().equals(identifier.getText())) {
                        variable.replaceElementWithVariable(litField.getIdentifier());
                        break;
                    }
                }
            }
            tpl.runInline();
        } else {
            inserted.navigate(true);
        }
    }

    @Nullable
    private RsStructItem buildStruct(@NotNull Project project, @NotNull Context ctx) {
        RsPsiFactory factory = new RsPsiFactory(project);
        String visibility = CreateFromUsageUtils.getVisibility(ctx.targetMod, RsElementUtil.getContainingMod((RsElement) ctx.structLiteral));
        String fields = generateFields(ctx.structLiteral, visibility);
        return factory.tryCreateStruct(visibility + "struct " + ctx.name + fields);
    }

    @Override
    @NotNull
    public IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    @NotNull
    public static String generateFields(@NotNull RsStructLiteral structLiteral, @NotNull String visibility) {
        List<RsStructLiteralField> fieldList = structLiteral.getStructLiteralBody().getStructLiteralFieldList();
        StringBuilder fieldsJoined = new StringBuilder();
        for (int i = 0; i < fieldList.size(); i++) {
            RsStructLiteralField f = fieldList.get(i);
            if (i > 0) fieldsJoined.append(",\n");
            String name = f.getReferenceName();
            RsExpr expr = f.getExpr();
            Ty type;
            if (expr != null) {
                type = RsTypesUtil.getType(expr);
            } else {
                RsPatBinding binding = RsStructLiteralFieldUtil.resolveToBinding(f);
                type = binding != null ? RsTypesUtil.getType(binding) : TyUnknown.INSTANCE;
            }
            fieldsJoined.append(visibility)
                .append(name)
                .append(": ")
                .append(TypeRendering.renderInsertionSafe(type, true, false, false));
        }
        return " {\n" + fieldsJoined + "\n}";
    }
}
