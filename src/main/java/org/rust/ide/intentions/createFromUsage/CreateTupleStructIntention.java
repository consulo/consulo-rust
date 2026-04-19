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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.inspections.lints.RsNamingInspection;
import org.rust.ide.intentions.RsElementBaseIntentionAction;
import org.rust.ide.presentation.RsPsiRenderingUtil;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.ide.utils.imports.ImportBridge;
import org.rust.ide.utils.template.EditorExt;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.types.ExtensionsUtil;

public class CreateTupleStructIntention extends RsElementBaseIntentionAction<CreateTupleStructIntention.Context> {
    @Override
    @NotNull
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.create.tuple.struct");
    }

    public static class Context {
        @NotNull
        public final String name;
        @NotNull
        public final RsCallExpr call;
        @NotNull
        public final RsMod targetMod;
        @NotNull
        public final PsiInsertionPlace place;

        public Context(@NotNull String name, @NotNull RsCallExpr call,
                        @NotNull RsMod targetMod, @NotNull PsiInsertionPlace place) {
            this.name = name;
            this.call = call;
            this.targetMod = targetMod;
            this.place = place;
        }
    }

    @Override
    @Nullable
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsPath path = RsElementUtil.contextStrict(element, RsPath.class);
        RsCallExpr functionCall = path != null ? RsElementUtil.contextStrict(path, RsCallExpr.class) : null;
        if (functionCall != null) {
            if (!RsElementUtil.isContextOf(functionCall.getExpr(), path)) return null;
            if (RsPathUtil.getResolveStatus(path) != PathResolveStatus.UNRESOLVED) return null;

            RsMod targetMod = CreateFromUsageUtils.getWritablePathMod(path);
            if (targetMod == null) return null;

            String name = path.getReferenceName();
            if (name == null) return null;
            if (!RsNamingInspection.isCamelCase(name)) return null;

            Ty expectedType = ExtensionsUtil.getExpectedType(functionCall);
            if (expectedType == null) expectedType = TyUnknown.INSTANCE;
            // Do not offer the intention if the expected type is known
            if (!(expectedType instanceof TyUnknown)) return null;
            PsiInsertionPlace place = PsiInsertionPlace.forItemInModBefore(targetMod, functionCall);
            if (place == null) return null;

            setText(RsBundle.message("intention.name.create.tuple.struct", name));
            return new Context(name, functionCall, targetMod, place);
        }
        return null;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        RsStructItem newStruct = buildStruct(project, ctx);
        if (newStruct == null) return;
        RsStructItem inserted = ctx.place.insert(newStruct);

        List<Ty> types = ctx.call.getValueArgumentList().getExprList().stream()
            .map(RsTypesUtil::getType)
            .collect(Collectors.toList());
        ImportBridge.importTypeReferencesFromTys(inserted, types);

        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        List<RsTupleFieldDecl> fields = inserted.getTupleFields() != null
            ? inserted.getTupleFields().getTupleFieldDeclList()
            : java.util.Collections.emptyList();
        if (EditorExt.canRunTemplateFor(editor, inserted) && !fields.isEmpty()) {
            List<RsInferType> unknownTypes = RsElementUtil.descendantsOfType(inserted, RsInferType.class);
            if (!unknownTypes.isEmpty()) {
                EditorExt.buildAndRunTemplate(editor, inserted, (Iterable<PsiElement>)(Iterable<?>) unknownTypes);
            } else {
                org.rust.openapiext.Editor.moveCaretToOffset(editor, fields.get(0), fields.get(0).getTextOffset());
            }
        } else {
            inserted.navigate(true);
        }
    }

    @Nullable
    private RsStructItem buildStruct(@NotNull Project project, @NotNull Context ctx) {
        RsPsiFactory factory = new RsPsiFactory(project);
        String visibility = CreateFromUsageUtils.getVisibility(ctx.targetMod, RsElementUtil.getContainingMod((RsElement) ctx.call));
        String fields = generateFields(ctx.call, visibility);
        return factory.tryCreateStruct(visibility + "struct " + ctx.name + fields + ";");
    }

    @Override
    @NotNull
    public IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    @NotNull
    public static String generateFields(@NotNull RsCallExpr call, @NotNull String visibility) {
        StringJoiner joiner = new StringJoiner(", ", "(", ")");
        for (RsExpr expr : call.getValueArgumentList().getExprList()) {
            joiner.add(visibility + TypeRendering.renderInsertionSafe(RsTypesUtil.getType(expr), true, false, false));
        }
        return joiner.toString();
    }
}
