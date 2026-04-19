/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsLetDecl;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class ConvertFunctionToClosureIntention extends RsElementBaseIntentionAction<ConvertFunctionToClosureIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.convert.function.to.closure");
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
        public final RsFunction targetFunction;

        public Context(RsFunction targetFunction) {
            this.targetFunction = targetFunction;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsFunction possibleTarget = RsPsiJavaUtil.contextStrict(element, RsFunction.class);
        if (possibleTarget == null) return null;

        int startOffset = possibleTarget.getFn().getTextRange().getStartOffset();
        int endOffset;
        if (possibleTarget.getRetType() != null) {
            endOffset = possibleTarget.getRetType().getTextRange().getEndOffset();
        } else if (possibleTarget.getValueParameterList() != null) {
            endOffset = possibleTarget.getValueParameterList().getTextRange().getEndOffset();
        } else {
            return null;
        }
        TextRange availabilityRange = new TextRange(startOffset, endOffset);
        if (!availabilityRange.containsOffset(element.getTextRange().getStartOffset())) return null;

        if (RsPsiJavaUtil.contextStrict(possibleTarget, RsFunction.class) == null) return null;
        if (possibleTarget.getTypeParameterList() != null) return null;
        if (!PsiModificationUtil.canReplace(possibleTarget)) return null;

        return new Context(possibleTarget);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        doInvoke(project, editor, ctx.targetFunction);
    }

    public void doInvoke(Project project, Editor editor, RsFunction function) {
        RsPsiFactory factory = new RsPsiFactory(project);

        List<RsValueParameter> rawParams = function.getRawValueParameters();
        String parametersText = rawParams.stream()
            .map(PsiElement::getText)
            .collect(Collectors.joining(", "));
        String returnText = function.getRetType() != null ? function.getRetType().getText() : "";

        String bodyText = RsFunctionUtil.getBlock(function) != null ? RsFunctionUtil.getBlock(function).getText() : null;
        if (bodyText == null) return;

        RsExpr lambda = factory.createLambda("|" + parametersText + "| " + returnText + " " + bodyText);
        RsLetDecl declaration = factory.createLetDeclaration(function.getIdentifier().getText(), lambda);

        RsLetDecl replaced = (RsLetDecl) function.replace(declaration);
        PsiElement semicolon = replaced.getSemicolon();
        if (semicolon != null && editor != null) {
            org.rust.openapiext.Editor.moveCaretToOffset(editor, replaced, semicolon.getTextRange().getEndOffset());
        }
    }
}
