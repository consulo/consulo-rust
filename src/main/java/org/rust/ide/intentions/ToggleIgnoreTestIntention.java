/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsOuterAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.RsAttrOwnerExtUtil;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;

public class ToggleIgnoreTestIntention extends RsElementBaseIntentionAction<ToggleIgnoreTestIntention.Context> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.toggle.ignore.for.tests");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final RsFunction myElement;

        public Context(@NotNull RsFunction element) {
            myElement = element;
        }

        @NotNull
        public RsFunction getElement() {
            return myElement;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof RsFunction)) return null;
        RsFunction function = (RsFunction) parent;
        if (!RsFunctionUtil.isTest(function)) return null;
        return new Context(function);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        RsOuterAttr existingIgnore = RsDocAndAttributeOwnerUtil.findOuterAttr(ctx.getElement(), "ignore");
        if (existingIgnore == null) {
            RsOuterAttr ignore = new RsPsiFactory(project).createOuterAttr("ignore");
            RsOuterAttr test = RsDocAndAttributeOwnerUtil.findOuterAttr(ctx.getElement(), "test");
            ctx.getElement().addBefore(ignore, test);
        } else {
            existingIgnore.delete();
        }
    }
}
