/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsPsiFactory;
import consulo.localize.LocalizeValue;

public class AddAttrParenthesesFix extends RsQuickFixBase<RsMetaItem> {
    private final String attrName;

    public AddAttrParenthesesFix(@Nonnull RsMetaItem element, @Nonnull String attrName) {
        super(element);
        this.attrName = attrName;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.add.parentheses"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.parentheses.to", attrName));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsMetaItem element) {
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
