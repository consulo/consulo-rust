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
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsMetaItemArgs;
import org.rust.lang.core.psi.ext.RsAttr;
import org.rust.lang.core.psi.ext.RsAttrUtil;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RemoveMetaItemFix extends RemoveElementFix {

    public RemoveMetaItemFix(@NotNull RsMetaItem metaItem) {
        super(metaItem, "feature" + (RsAttrUtil.getName(metaItem) != null
            ? " `" + RsAttrUtil.getName(metaItem) + "`"
            : ""));
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        if (!(element instanceof RsMetaItem)) return;
        RsMetaItem metaItem = (RsMetaItem) element;
        PsiElement parent = metaItem.getParent();
        if (!(parent instanceof RsMetaItemArgs)) return;
        RsMetaItemArgs arguments = (RsMetaItemArgs) parent;
        int size = arguments.getMetaItemList().size();
        if (size == 0) return;
        if (size == 1) {
            PsiElement grandParent = arguments.getParent();
            if (grandParent != null) {
                PsiElement greatGrandParent = grandParent.getParent();
                if (greatGrandParent instanceof RsAttr) {
                    greatGrandParent.delete();
                }
            }
        } else {
            PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(metaItem);
        }
    }
}
