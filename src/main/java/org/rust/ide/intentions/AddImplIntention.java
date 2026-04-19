/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.utils.PsiInsertionPlace;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

public class AddImplIntention extends RsElementBaseIntentionAction<AddImplIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.impl.block");
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
        PsiElement newImpl = new RsPsiFactory(project).createInherentImplItem(ctx.typeName, ctx.type.getTypeParameterList(), ctx.type.getWhereClause());
        PsiElement insertedImpl = ctx.placeForImpl.insert(newImpl);
        org.rust.openapiext.Editor.moveCaretToOffset(editor, insertedImpl, insertedImpl.getTextRange().getEndOffset() - 1);
    }
}
