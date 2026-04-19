/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.util.text.CharArrayUtil;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsGenericDeclaration;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.psi.impl.RsFunctionImpl;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsValueParameterUtil;

public class GenerateDocStubIntention extends RsElementBaseIntentionAction<GenerateDocStubIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.generate.documentation.stub");
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
        public final RsElement func;
        public final List<RsValueParameter> params;
        public final Ty returnType;

        public Context(RsElement func, List<RsValueParameter> params, Ty returnType) {
            this.func = func;
            this.params = params;
            this.returnType = returnType;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsGenericDeclaration targetFunc = RsPsiJavaUtil.ancestorOrSelf(element, RsGenericDeclaration.class);
        if (!(targetFunc instanceof RsFunctionImpl)) return null;
        RsFunctionImpl func = (RsFunctionImpl) targetFunc;
        if (!func.getName().equals(element.getText())) return null;
        if (func.getText().startsWith("///")) return null;
        List<RsValueParameter> params = func.getValueParameters();
        if (params.isEmpty()) return null;
        Ty returnType = RsFunctionUtil.getRawReturnType(func);
        return new Context(func, params, returnType);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsElement targetFunc = ctx.func;
        List<RsValueParameter> params = ctx.params;
        Ty returnType = ctx.returnType;

        Document document = editor.getDocument();
        int commentStartOffset = targetFunc.getTextRange().getStartOffset();
        int lineNum = document.getLineNumber(commentStartOffset);
        int lineStartOffset = document.getLineStartOffset(lineNum);
        if (lineStartOffset >= 1 && lineStartOffset < commentStartOffset) {
            int nonWhiteSpaceOffset = CharArrayUtil.shiftBackward(document.getCharsSequence(), commentStartOffset - 1, " \t");
            commentStartOffset = Math.max(nonWhiteSpaceOffset, lineStartOffset);
        }

        StringBuilder buffer = new StringBuilder();
        int indentOffset = targetFunc.getTextRange().getStartOffset() - lineStartOffset;
        String indentation = " ".repeat(indentOffset);
        buffer.append(indentation).append("/// \n");
        int commentBodyRelativeOffset = buffer.length();

        document.insertString(commentStartOffset, buffer.toString());
        PsiDocumentManager docManager = PsiDocumentManager.getInstance(project);
        docManager.commitDocument(document);

        String stub = generateDocumentStub(indentation, params, returnType);
        if (!stub.isEmpty()) {
            int insertionOffset = commentStartOffset + commentBodyRelativeOffset;
            document.insertString(insertionOffset, stub);
            docManager.commitDocument(document);
        }
        org.rust.openapiext.Editor.moveCaretToOffset(editor, targetFunc, targetFunc.getTextRange().getStartOffset() + buffer.length() - indentOffset - 1);
    }

    private static String generateDocumentStub(String indentation, List<RsValueParameter> params, Ty returnType) {
        StringBuilder sb = new StringBuilder();
        sb.append(indentation).append("/// \n");
        sb.append(indentation).append("/// # Arguments \n");
        sb.append(indentation).append("/// \n");
        for (RsValueParameter param : params) {
            sb.append(indentation).append("/// * `").append(RsValueParameterUtil.getPatText(param)).append("`: \n");
        }
        sb.append(indentation).append("/// \n");
        sb.append(indentation).append("/// returns: ").append(returnType).append(" \n");
        sb.append(indentation).append("/// \n");
        sb.append(indentation).append("/// # Examples \n");
        sb.append(indentation).append("/// \n");
        sb.append(indentation).append("/// ```\n");
        sb.append(indentation).append("/// \n");
        sb.append(indentation).append("/// ```\n");
        return sb.toString();
    }
}
