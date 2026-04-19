/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.assist;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.lang.SmartEnterProcessorWithFixers;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.Arrays;
import java.util.List;

public class FunctionOrStructFixer extends SmartEnterProcessorWithFixers.Fixer<RsSmartEnterProcessor> {

    private static final List<IElementType> APPROVED_TYPES = Arrays.asList(
        RsElementTypes.IDENTIFIER, RsElementTypes.VALUE_PARAMETER_LIST, RsElementTypes.RET_TYPE
    );

    @Override
    public void apply(Editor editor, RsSmartEnterProcessor processor, PsiElement element) {
        IElementType elementType = RsElementUtil.getElementType(element);
        if (!APPROVED_TYPES.contains(elementType)) return;

        PsiElement parent = element.getParent();
        String prefix;
        if (parent instanceof RsFunction) {
            RsFunction function = (RsFunction) parent;
            if (RsFunctionUtil.getBlock(function) != null) {
                prefix = null;
            } else if (function.getValueParameterList() == null) {
                prefix = "()";
            } else if (!(RsFunctionUtil.getRawReturnType(function) instanceof TyUnknown)) {
                prefix = "";
            } else {
                prefix = null;
            }
        } else if (isStructOrUnionAndIdentifier(parent)) {
            prefix = "";
        } else {
            prefix = null;
        }

        if (prefix == null) return;
        editor.getDocument().insertString(parent.getTextRange().getEndOffset(), prefix + " { }");
    }

    private static boolean isStructOrUnionAndIdentifier(PsiElement element) {
        if (!(element instanceof RsStructItem)) return false;
        RsStructItem structItem = (RsStructItem) element;
        return structItem.getTupleFields() == null && structItem.getBlockFields() == null;
    }
}
