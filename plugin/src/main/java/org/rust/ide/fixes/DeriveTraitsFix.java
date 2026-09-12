/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsAttrUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsStructOrEnumItemElement;
import consulo.localize.LocalizeValue;

public class DeriveTraitsFix extends RsQuickFixBase<RsStructOrEnumItemElement> {

    private final String traits;
    @Nullable
    private final String itemName;

    public DeriveTraitsFix(@Nonnull RsStructOrEnumItemElement item, @Nonnull String traits) {
        super(item);
        this.traits = traits;
        this.itemName = item.getName();
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.add.derive.to", traits, itemName != null ? itemName : ""));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.derive.trait"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsStructOrEnumItemElement element) {
        invokeStatic(element, traits);
    }

    public static void invokeStatic(@Nonnull RsStructOrEnumItemElement item, @Nonnull String traits) {
        RsPsiFactory factory = new RsPsiFactory(item.getProject());
        RsOuterAttr existingDeriveAttr = RsAttrUtil.findOuterAttr(item, "derive");

        if (existingDeriveAttr != null) {
            updateDeriveAttr(factory, existingDeriveAttr, traits);
        } else {
            createDeriveAttr(factory, item, traits);
        }
    }

    private static void updateDeriveAttr(@Nonnull RsPsiFactory psiFactory, @Nonnull RsOuterAttr deriveAttr, @Nonnull String traits) {
        String oldAttrText = deriveAttr.getMetaItem().getText();
        String newAttrText = oldAttrText.substring(0, oldAttrText.lastIndexOf(')')) + ", " + traits + ")";
        var newDeriveAttr = psiFactory.createMetaItem(newAttrText);
        deriveAttr.getMetaItem().replace(newDeriveAttr);
    }

    private static void createDeriveAttr(@Nonnull RsPsiFactory psiFactory, @Nonnull RsStructOrEnumItemElement item, @Nonnull String traits) {
        PsiElement keyword = RsElementUtil.firstKeyword(item);
        assert keyword != null;
        String newAttrText = "derive(" + traits + ")";
        var newDeriveAttr = psiFactory.createOuterAttr(newAttrText);
        item.addBefore(newDeriveAttr, keyword);
    }
}
