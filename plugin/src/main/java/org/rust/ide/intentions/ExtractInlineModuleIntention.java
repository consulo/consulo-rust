/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;
import consulo.language.psi.util.PsiTreeUtil;
import org.rust.openapiext.Testmark;
import consulo.localize.LocalizeValue;

public class ExtractInlineModuleIntention extends RsElementBaseIntentionAction<RsModItem> {

    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.extract.inline.module.structure"));
        }

    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.extract.inline.module"));
        }

    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Override
    public RsModItem findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsModItem mod = RsPsiJavaUtil.ancestorOrSelf(element, RsModItem.class);
        if (mod == null) return null;
        if (element != mod.getMod() && element != mod.getIdentifier()
            && !(mod.getVis() != null && PsiTreeUtil.isAncestor(mod.getVis(), element, false))) return null;
        if (!PsiModificationUtil.canReplace(mod)) return null;
        return mod;
    }

    @Override
    public void invoke(Project project, Editor editor, RsModItem ctx) {
        String modName = ctx.getName();
        if (modName == null) return;
        RsModDeclItem decl = new RsPsiFactory(project).createModDeclItem(modName);
        PsiElement parent = ctx.getParent();
        if (parent == null) return;
        decl = (RsModDeclItem) parent.addBefore(decl, ctx);

        if (ctx.getFirstChild() != ctx.getMod()) {
            Testmarks.CopyAttrs.hit();
            decl.addRangeBefore(ctx.getFirstChild(), ctx.getMod().getPrevSibling(), decl.getMod());
        }

        PsiFile modFile = RsPsiJavaUtil.getOrCreateModuleFile(decl);
        if (modFile == null) return;

        PsiElement startElement = ctx.getLbrace().getNextSibling();
        if (startElement == null) return;
        PsiElement rbrace = ctx.getRbrace();
        PsiElement endElement = rbrace != null ? rbrace.getPrevSibling() : null;
        if (endElement == null) return;

        modFile.addRange(startElement, endElement);
        ctx.delete();
    }

    public IntentionPreviewInfo generatePreview(Project project, Editor editor, PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    public static class Testmarks {
        public static final Testmark CopyAttrs = new Testmark();
    }
}
