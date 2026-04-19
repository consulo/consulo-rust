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
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.RsUseSpeck;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.psi.ext.RsUseSpeckUtil;

/**
 * Adds curly braces to singleton imports, changing from this
 *
 * <pre>
 * import std::mem;
 * </pre>
 *
 * to this:
 *
 * <pre>
 * import std::{mem};
 * </pre>
 */
public class AddCurlyBracesIntention extends RsElementBaseIntentionAction<AddCurlyBracesIntention.Context> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.curly.braces");
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
        public final RsUseSpeck useSpeck;
        public final RsPath path;
        public final PsiElement semicolon;

        public Context(RsUseSpeck useSpeck, RsPath path, PsiElement semicolon) {
            this.useSpeck = useSpeck;
            this.path = path;
            this.semicolon = semicolon;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsUseItem useItem = RsPsiJavaUtil.ancestorStrict(element, RsUseItem.class);
        if (useItem == null) return null;
        PsiElement semicolon = useItem.getSemicolon();
        if (semicolon == null) return null;
        RsUseSpeck useSpeck = useItem.getUseSpeck();
        if (useSpeck == null) return null;
        RsPath path = useSpeck.getPath();
        if (path == null) return null;
        if (useSpeck.getUseGroup() != null || RsUseSpeckUtil.isStarImport(useSpeck)) return null;
        return new Context(useSpeck, path, semicolon);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        PsiElement referenceNameElement = ctx.path.getReferenceNameElement();
        String identifier = referenceNameElement != null ? referenceNameElement.getText() : "";

        // Create a new use item that contains a glob list that we can use.
        // Then extract from it the glob list and the double colon.
        RsUseSpeck newUseSpeck = new RsPsiFactory(project).createUseSpeck("dummy::{" + identifier + "}");
        PsiElement newGroup = newUseSpeck.getUseGroup();
        if (newGroup == null) return;
        PsiElement newColonColon = newUseSpeck.getColoncolon();
        if (newColonColon == null) return;

        PsiElement alias = ctx.useSpeck.getAlias();

        // If there was an alias before, insert it into the new glob item
        if (alias != null) {
            PsiElement newGlobItem = newGroup.getChildren()[0];
            newGlobItem.addAfter(alias, newGlobItem.getLastChild());
        }

        // Remove the identifier from the path by replacing it with its subpath
        RsPath qualifier = ctx.path.getPath();
        if (qualifier != null) {
            ctx.path.replace(qualifier);
        } else {
            ctx.path.delete();
        }

        // Delete the alias of the identifier, if any
        if (alias != null) {
            alias.delete();
        }

        // Insert the double colon and glob list into the use item
        ctx.useSpeck.add(newColonColon);
        ctx.useSpeck.add(newGroup);

        org.rust.openapiext.Editor.moveCaretToOffset(editor, ctx.semicolon, ctx.semicolon.getTextRange().getStartOffset() - 1);
    }
}
