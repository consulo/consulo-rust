/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsPsiFactory;

public class AddAttrParenthesesFix extends RsQuickFixBase<RsMetaItem> {
    private final String attrName;

    public AddAttrParenthesesFix(@NotNull RsMetaItem element, @NotNull String attrName) {
        super(element);
        this.attrName = attrName;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.add.parentheses");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.parentheses.to", attrName);
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsMetaItem element) {
        RsMetaItem newItem = new RsPsiFactory(project).createOuterAttr(attrName + "()").getMetaItem();
        RsMetaItem replaced = (RsMetaItem) element.replace(newItem);

        if (replaced.getMetaItemArgs() == null) return;
        var lparen = replaced.getMetaItemArgs().getLparen();
        if (lparen == null) return;
        if (editor != null) {
            editor.getCaretModel().moveToOffset(lparen.getTextOffset() + 1);
        }
    }
}
