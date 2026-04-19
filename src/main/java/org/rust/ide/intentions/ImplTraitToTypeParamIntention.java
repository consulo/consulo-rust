/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.template.EditorExt;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.Testmark;

import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.RsPsiImplUtil;

public class ImplTraitToTypeParamIntention extends RsElementBaseIntentionAction<ImplTraitToTypeParamIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.convert.impl.trait.to.type.parameter");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        public final RsTraitType argType;
        public final RsFunction fnSignature;

        public Context(RsTraitType argType, RsFunction fnSignature) {
            this.argType = argType;
            this.fnSignature = fnSignature;
        }
    }

    public static final Testmark OuterImplTestMark = new Testmark();

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsTraitType traitType = RsPsiJavaUtil.ancestorStrict(element, RsTraitType.class);
        if (traitType == null) return null;
        if (traitType.getImpl() == null) return null;
        RsValueParameterList paramList = RsPsiJavaUtil.ancestorStrict(element, RsValueParameterList.class);
        if (paramList == null) return null;
        PsiElement parent = paramList.getParent();
        if (!(parent instanceof RsFunction)) return null;
        return new Context(traitType, (RsFunction) parent);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        RsTraitType argType = ctx.argType;
        RsFunction fnSignature = ctx.fnSignature;

        List<RsTraitType> innerTraitTypes = RsPsiJavaUtil.descendantsOfType(argType, RsTraitType.class);
        boolean hasInnerImpl = innerTraitTypes.stream().anyMatch(t -> t.getImpl() != null);
        if (hasInnerImpl) {
            OuterImplTestMark.hit();
            if (RsPsiImplUtil.isIntentionPreviewElement(fnSignature)) return;
            org.rust.openapiext.Editor.showErrorHint(editor, RsBundle.message("hint.text.please.convert.innermost.impl.trait.first"), HintManager.UNDER);
            return;
        }

        RsTypeParameterList typeParameterList = fnSignature.getTypeParameterList();
        if (typeParameterList == null) {
            typeParameterList = (RsTypeParameterList) fnSignature.addAfter(psiFactory.createTypeParameterList(""), fnSignature.getIdentifier());
        }

        PsiElement anchor = !typeParameterList.getConstParameterList().isEmpty()
            ? typeParameterList.getConstParameterList().get(0)
            : typeParameterList.getGt();

        String typeParameterName = "T";
        String boundsText = argType.getPolyboundList().stream()
            .map(PsiElement::getText)
            .collect(Collectors.joining("+"));

        RsTypeParameter typeParameter = psiFactory
            .createTypeParameterList(typeParameterName + ":" + boundsText)
            .getTypeParameterList()
            .get(0);

        typeParameter = (RsTypeParameter) typeParameterList.addBefore(typeParameter, anchor);

        PsiElement prev = RsPsiJavaUtil.getPrevNonWhitespaceSibling(typeParameter);
        PsiElement next = RsPsiJavaUtil.getNextNonWhitespaceSibling(typeParameter);
        if (prev != typeParameterList.getLt() && RsPsiJavaUtil.elementType(prev) != RsElementTypes.COMMA) {
            typeParameterList.addBefore(psiFactory.createComma(), typeParameter);
        }
        if (next != typeParameterList.getGt() && RsPsiJavaUtil.elementType(next) != RsElementTypes.COMMA) {
            typeParameterList.addAfter(psiFactory.createComma(), typeParameter);
        }

        PsiElement newArgType = argType.replace(psiFactory.createType(typeParameterName));
        var newArgTypePtr = OpenApiUtil.createSmartPointer(newArgType);

        var tpl = EditorExt.newTemplateBuilder(editor, fnSignature);
        var variable = tpl.introduceVariable(typeParameter.getIdentifier(), typeParameterName);
        PsiElement resolvedNewArgType = newArgTypePtr.getElement();
        if (resolvedNewArgType != null) {
            variable.replaceElementWithVariable(resolvedNewArgType);
        }
        tpl.withExpressionsHighlighting();
        tpl.runInline();
    }
}
