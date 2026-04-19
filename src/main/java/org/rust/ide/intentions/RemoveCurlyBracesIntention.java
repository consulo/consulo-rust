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
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsUseGroupUtil;
import org.rust.lang.core.psi.ext.RsUseSpeckUtil;

public class RemoveCurlyBracesIntention extends RsElementBaseIntentionAction<RemoveCurlyBracesIntention.Context> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.remove.curly.braces");
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
        private final RsPath myPath;
        private final RsUseGroup myUseGroup;
        private final String myName;

        public Context(@NotNull RsPath path, @NotNull RsUseGroup useGroup, @NotNull String name) {
            myPath = path;
            myUseGroup = useGroup;
            myName = name;
        }

        @NotNull
        public RsPath getPath() {
            return myPath;
        }

        @NotNull
        public RsUseGroup getUseGroup() {
            return myUseGroup;
        }

        @NotNull
        public String getName() {
            return myName;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            if (current instanceof RsUseSpeck) {
                Context ctx = createContextIfCompatible((RsUseSpeck) current);
                if (ctx != null) return ctx;
            }
            current = current.getParent();
        }
        return null;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        int caret = editor.getCaretModel().getOffset();
        int groupStart = PsiElementExt.getStartOffset(ctx.getUseGroup());
        int groupEnd = PsiElementExt.getEndOffset(ctx.getUseGroup());
        int newOffset;
        if (caret < groupStart) {
            newOffset = caret;
        } else if (caret < groupEnd) {
            newOffset = caret - 1;
        } else {
            newOffset = caret - 2;
        }

        removeCurlyBracesFromUseSpeck(ctx);
        editor.getCaretModel().moveToOffset(newOffset);
    }

    @Nullable
    public static Context createContextIfCompatible(@NotNull RsUseSpeck useSpeck) {
        RsUseGroup useGroup = useSpeck.getUseGroup();
        if (useGroup == null) return null;
        RsPath path = useSpeck.getPath();
        if (path == null) return null;
        RsUseSpeck trivial = RsUseGroupUtil.getAsTrivial(useGroup);
        if (trivial == null) return null;
        String name = trivial.getText();
        if (!PsiModificationUtil.canReplace(useGroup)) return null;
        return new Context(path, useGroup, name);
    }

    public static void removeCurlyBracesFromUseSpeck(@NotNull Context ctx) {
        RsPath path = ctx.getPath();
        RsUseGroup useGroup = ctx.getUseGroup();
        String name = ctx.getName();

        RsPsiFactory factory = new RsPsiFactory(useGroup.getProject());
        RsUseSpeck newUseSpeck = factory.createUseSpeck("dummy::" + name);
        RsPath newPath = newUseSpeck.getPath();
        if (newPath == null) return;
        RsPath newSubPath = RsPathUtil.basePath(newPath);

        newSubPath.replace(path.copy());
        path.replace(newPath);
        PsiElement coloncolon = RsUseSpeckUtil.getParentUseSpeck(useGroup).getColoncolon();
        if (coloncolon != null) coloncolon.delete();
        RsAlias alias = newUseSpeck.getAlias();
        if (alias != null) {
            useGroup.replace(alias.copy());
        } else {
            useGroup.delete();
        }
    }
}
