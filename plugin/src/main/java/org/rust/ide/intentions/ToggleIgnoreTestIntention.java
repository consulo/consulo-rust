/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.toggle.ignore.for.tests"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final RsFunction myElement;

        public Context(@Nonnull RsFunction element) {
            myElement = element;
        }

        @Nonnull
        public RsFunction getElement() {
            return myElement;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof RsFunction)) return null;
        RsFunction function = (RsFunction) parent;
        if (!RsFunctionUtil.isTest(function)) return null;
        return new Context(function);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
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
