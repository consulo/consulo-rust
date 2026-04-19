/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

public class AddDeriveIntention extends RsElementBaseIntentionAction<AddDeriveIntention.Context> {

    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.name.add.derive.clause");
    }

    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.derive.clause");
    }

    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        public final RsStructOrEnumItemElement item;
        public final PsiElement itemStart;

        public Context(RsStructOrEnumItemElement item, PsiElement itemStart) {
            this.item = item;
            this.itemStart = itemStart;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsStructOrEnumItemElement item = RsPsiJavaUtil.ancestorStrict(element, RsStructOrEnumItemElement.class);
        if (item == null) return null;
        PsiElement keyword = RsPsiJavaUtil.firstKeyword(item);
        if (keyword == null) return null;
        return new Context(item, keyword);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsOuterAttr deriveAttr = findOrCreateDeriveAttr(project, ctx.item, ctx.itemStart);
        moveCaret(editor, deriveAttr);
    }

    private RsOuterAttr findOrCreateDeriveAttr(Project project, RsStructOrEnumItemElement item, PsiElement keyword) {
        RsOuterAttr existingDeriveAttr = RsPsiJavaUtil.findOuterAttr(item, "derive");
        if (existingDeriveAttr != null) {
            return existingDeriveAttr;
        }

        RsOuterAttr attr = new RsPsiFactory(project).createOuterAttr("derive()");
        return (RsOuterAttr) item.addBefore(attr, keyword);
    }

    private void moveCaret(Editor editor, RsOuterAttr deriveAttr) {
        int offset;
        if (deriveAttr.getMetaItem().getMetaItemArgs() != null && deriveAttr.getMetaItem().getMetaItemArgs().getRparen() != null) {
            offset = deriveAttr.getMetaItem().getMetaItemArgs().getRparen().getTextOffset();
        } else {
            offset = deriveAttr.getRbrack().getTextOffset();
        }
        org.rust.openapiext.Editor.moveCaretToOffset(editor, deriveAttr, offset);
    }
}
