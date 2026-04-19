/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInsight.template.impl.MacroCallNode;
import com.intellij.codeInsight.template.macro.CompleteMacro;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import org.rust.RsBundle;
import org.rust.ide.fixes.AddGenericArguments;
import org.rust.ide.refactoring.implementMembers.ImplementMembersImpl;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.ide.utils.template.EditorExt;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;

import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.BoundElement;

public class AddImplTraitIntention extends RsElementBaseIntentionAction<AddImplTraitIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.implement.trait");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    public static class Context {
        public final RsStructOrEnumItemElement type;
        public final String typeName;
        public final PsiInsertionPlace placeForImpl;

        public Context(RsStructOrEnumItemElement type, String typeName, PsiInsertionPlace placeForImpl) {
            this.type = type;
            this.typeName = typeName;
            this.placeForImpl = placeForImpl;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsStructOrEnumItemElement struct = RsPsiJavaUtil.ancestorStrict(element, RsStructOrEnumItemElement.class);
        if (struct == null) return null;
        String typeName = struct.getName();
        if (typeName == null) return null;
        PsiInsertionPlace placeForImpl = PsiInsertionPlace.forItemInTheScopeOf(struct);
        if (placeForImpl == null) return null;
        return new Context(struct, typeName, placeForImpl);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsImplItem newImpl = new RsPsiFactory(project).createTraitImplItem(
            ctx.typeName,
            "T",
            ctx.type.getTypeParameterList(),
            ctx.type.getWhereClause()
        );

        RsImplItem insertedImpl = (RsImplItem) ctx.placeForImpl.insert(newImpl);
        RsTraitRef traitRef = insertedImpl.getTraitRef();
        if (traitRef == null) return;
        RsPath traitName = traitRef.getPath();
        if (traitName == null) return;

        SmartPsiElementPointer<RsImplItem> implPtr = OpenApiUtil.createSmartPointer(insertedImpl);
        EditorExt.newTemplateBuilder(editor, insertedImpl)
            .replaceElement(traitName, new MacroCallNode(new CompleteMacro()))
            .withDisabledDaemonHighlighting()
            .runInline(() -> {
                RsImplItem implCurrent = implPtr.getElement();
                if (implCurrent != null) {
                    ApplicationManager.getApplication().runWriteAction(() -> {
                        afterTraitNameEntered(implCurrent, editor);
                    });
                }
            });
    }

    private void afterTraitNameEntered(RsImplItem impl, Editor editor) {
        RsTraitRef traitRef = impl.getTraitRef();
        if (traitRef == null) return;
        Object trait = RsTraitRefUtil.resolveToBoundTrait(traitRef);
        if (trait == null) return;

        List<SmartPsiElementPointer<RsElement>> insertedGenericArgumentsPtr = null;
        if (!RsGenericDeclarationUtil.getRequiredGenericParameters((RsTraitItem) ((BoundElement<?>) trait).getElement()).isEmpty()) {
            List<RsElement> args = AddGenericArguments.insertGenericArgumentsIfNeeded(traitRef.getPath());
            if (args != null) {
                insertedGenericArgumentsPtr = args.stream()
                    .map(org.rust.openapiext.SmartPointerUtil::createSmartPointer)
                    .collect(Collectors.toList());
            }
        }

        ImplementMembersImpl.generateMissingTraitMembers(impl, traitRef, editor);

        RsImplItem restoredImpl = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(impl);
        if (restoredImpl == null) return;
        showGenericArgumentsTemplate(editor, restoredImpl, insertedGenericArgumentsPtr);
    }

    private void showGenericArgumentsTemplate(
        Editor editor,
        RsImplItem impl,
        List<SmartPsiElementPointer<RsElement>> insertedGenericArgumentsPtr
    ) {
        if (insertedGenericArgumentsPtr == null) return;

        List<RsPathType> insertedGenericArguments = new ArrayList<>();
        for (SmartPsiElementPointer<RsElement> ptr : insertedGenericArgumentsPtr) {
            RsElement el = ptr.getElement();
            if (el instanceof RsPathType) {
                insertedGenericArguments.add((RsPathType) el);
            }
        }

        if (insertedGenericArguments.isEmpty()) return;

        RsMembers members = impl.getMembers();
        if (members == null) return;

        List<RsPath> allPaths = RsPsiJavaUtil.descendantsOfType(members, RsPath.class);
        Map<String, List<RsPath>> pathTypes = new LinkedHashMap<>();
        for (RsPath path : allPaths) {
            if ((path.getParent() instanceof RsPathType || path.getParent() instanceof RsPathExpr)
                && !path.getHasColonColon() && path.getPath() == null && path.getTypeQual() == null) {
                String refName = path.getReferenceName();
                if (refName != null) {
                    pathTypes.computeIfAbsent(refName, k -> new ArrayList<>()).add(path);
                }
            }
        }

        Map<RsPathType, List<RsPath>> typeToUsage = new LinkedHashMap<>();
        for (RsPathType ty : insertedGenericArguments) {
            String refName = ty.getPath().getReferenceName();
            List<RsPath> usages = refName != null ? pathTypes.getOrDefault(refName, List.of()) : List.of();
            typeToUsage.put(ty, usages);
        }

        var tpl = EditorExt.newTemplateBuilder(editor, impl);
        for (Map.Entry<RsPathType, List<RsPath>> entry : typeToUsage.entrySet()) {
            var variable = tpl.introduceVariable(entry.getKey());
            for (RsPath usage : entry.getValue()) {
                variable.replaceElementWithVariable(usage);
            }
        }
        tpl.withExpressionsHighlighting();
        tpl.withDisabledDaemonHighlighting();
        tpl.runInline();
    }

    // No intention preview because user has to choose trait manually
    @Override
    public IntentionPreviewInfo generatePreview(Project project, Editor editor, PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }
}
