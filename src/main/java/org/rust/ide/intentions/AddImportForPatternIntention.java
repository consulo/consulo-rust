/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.rust.RsBundle;
import org.rust.ide.inspections.imports.AutoImportFixFactory;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsPatIdent;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

public class AddImportForPatternIntention extends RsElementBaseIntentionAction<AddImportForPatternIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.import");
    }

    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.add.import.for.path.in.pattern");
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public PsiFile getElementToMakeWritable(PsiFile currentFile) {
        return currentFile;
    }

    public static class Context {
        public final RsPatIdent pat;
        /** An AutoImportFix.Context instance stored as Object because the class
         *  resides in a package named "import" (Java reserved keyword). */
        public final Object autoImportContext;

        public Context(RsPatIdent pat, Object autoImportContext) {
            this.pat = pat;
            this.autoImportContext = autoImportContext;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsPatBinding pat = RsPsiJavaUtil.ancestorOrSelf(element, RsPatBinding.class);
        if (pat == null) return null;
        if (pat.getBindingMode() != null) return null;
        PsiElement parent = pat.getParent();
        if (!(parent instanceof RsPatIdent)) return null;
        RsPatIdent patIdent = (RsPatIdent) parent;
        RsMatchArm matchArm = RsPsiJavaUtil.ancestorStrict(patIdent, RsMatchArm.class);
        if (matchArm == null) return null;
        if (!com.intellij.psi.util.PsiTreeUtil.isAncestor(matchArm.getPat(), patIdent, false)) return null;
        if (!pat.getReference().multiResolve().isEmpty()) return null;

        var context = AutoImportFixFactory.findApplicableContext(pat);
        if (context == null) return null;
        return new Context(patIdent, context);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        AutoImportFixFactory.invoke(ctx.pat, ctx.autoImportContext, project, null);
    }
}
