/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.RemoveCurlyBracesIntention;
import org.rust.lang.core.psi.RsUseGroup;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.RsUseSpeck;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsUseSpeckUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;

/**
 * Fix that removes a use speck or a whole use item.
 */
public class RemoveImportFix extends RsQuickFixBase<PsiElement> {

    public RemoveImportFix(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.remove.unused.import");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        if (!(element instanceof RsElement)) return;
        deleteUseSpeckOrUseItem((RsElement) element);
    }

    private static void deleteUseSpeckOrUseItem(@NotNull RsElement element) {
        PsiElement parent = element.getParent();
        PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(element);

        if (parent instanceof RsUseGroup) {
            RsUseGroup useGroup = (RsUseGroup) parent;
            RsUseSpeck parentSpeck = RsUseSpeckUtil.getParentUseSpeck(useGroup);
            if (useGroup.getUseSpeckList().isEmpty()) {
                deleteUseSpeck(parentSpeck);
            } else {
                var ctx = RemoveCurlyBracesIntention.createContextIfCompatible(parentSpeck);
                if (ctx != null) {
                    RemoveCurlyBracesIntention.removeCurlyBracesFromUseSpeck(ctx);
                }
            }
        }
    }

    public static void deleteUseSpeck(@NotNull RsUseSpeck useSpeck) {
        PsiElement parent = useSpeck.getParent();
        RsElement element = parent instanceof RsUseItem ? (RsElement) parent : useSpeck;
        deleteUseSpeckOrUseItem(element);
    }
}
