/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsMetaItemArgs;
import org.rust.lang.core.psi.ext.RsAttr;
import org.rust.lang.core.psi.ext.RsAttrUtil;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RemoveMetaItemFix extends RemoveElementFix {

    public RemoveMetaItemFix(@Nonnull RsMetaItem metaItem) {
        super(metaItem, "feature" + (RsAttrUtil.getName(metaItem) != null
            ? " `" + RsAttrUtil.getName(metaItem) + "`"
            : ""));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
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
