/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsAttrUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;

public class DeriveTraitsFix extends RsQuickFixBase<RsStructOrEnumItemElement> {

    private final String traits;
    @Nullable
    private final String itemName;

    public DeriveTraitsFix(@NotNull RsStructOrEnumItemElement item, @NotNull String traits) {
        super(item);
        this.traits = traits;
        this.itemName = item.getName();
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.add.derive.to", traits, itemName != null ? itemName : "");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.derive.trait");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsStructOrEnumItemElement element) {
        invokeStatic(element, traits);
    }

    public static void invokeStatic(@NotNull RsStructOrEnumItemElement item, @NotNull String traits) {
        RsPsiFactory factory = new RsPsiFactory(item.getProject());
        RsOuterAttr existingDeriveAttr = RsAttrUtil.findOuterAttr(item, "derive");

        if (existingDeriveAttr != null) {
            updateDeriveAttr(factory, existingDeriveAttr, traits);
        } else {
            createDeriveAttr(factory, item, traits);
        }
    }

    private static void updateDeriveAttr(@NotNull RsPsiFactory psiFactory, @NotNull RsOuterAttr deriveAttr, @NotNull String traits) {
        String oldAttrText = deriveAttr.getMetaItem().getText();
        String newAttrText = oldAttrText.substring(0, oldAttrText.lastIndexOf(')')) + ", " + traits + ")";
        var newDeriveAttr = psiFactory.createMetaItem(newAttrText);
        deriveAttr.getMetaItem().replace(newDeriveAttr);
    }

    private static void createDeriveAttr(@NotNull RsPsiFactory psiFactory, @NotNull RsStructOrEnumItemElement item, @NotNull String traits) {
        PsiElement keyword = RsElementUtil.firstKeyword(item);
        assert keyword != null;
        String newAttrText = "derive(" + traits + ")";
        var newDeriveAttr = psiFactory.createOuterAttr(newAttrText);
        item.addBefore(newDeriveAttr, keyword);
    }
}
