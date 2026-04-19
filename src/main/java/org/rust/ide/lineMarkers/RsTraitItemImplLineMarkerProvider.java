/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsAbstractableUtil;
import org.rust.lang.core.psi.ext.RsElementUtil;

import javax.swing.Icon;
import java.util.Collection;
import java.util.Collections;

public class RsTraitItemImplLineMarkerProvider extends RelatedItemLineMarkerProvider {

    private final Option myImplementingOption = new Option(
        "rust.implementing.item",
        RsBundle.message("gutter.rust.implementing.item"),
        RsIcons.IMPLEMENTING_METHOD
    );

    private final Option myOverridingOption = new Option(
        "rust.overriding.item",
        RsBundle.message("gutter.rust.overriding.item"),
        RsIcons.OVERRIDING_METHOD
    );

    @NotNull
    @Override
    public String getName() {
        return "";
    }

    @NotNull
    @Override
    public Option[] getOptions() {
        return new Option[]{myImplementingOption, myOverridingOption};
    }

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement el, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(el instanceof RsAbstractable)) return;
        RsAbstractable abstractable = (RsAbstractable) el;

        PsiElement superItem = RsAbstractableUtil.getSuperItem(abstractable);
        if (superItem == null) return;
        RsTraitItem trait = RsElementUtil.ancestorStrict(superItem, RsTraitItem.class);
        if (trait == null) return;

        String action;
        Icon icon;
        if (((RsAbstractable) superItem).isAbstract()) {
            if (!myImplementingOption.isEnabled()) return;
            action = RsBundle.message("tooltip.implements");
            icon = RsIcons.IMPLEMENTING_METHOD;
        } else {
            if (!myOverridingOption.isEnabled()) return;
            action = RsBundle.message("tooltip.overrides");
            icon = RsIcons.OVERRIDING_METHOD;
        }

        String type;
        PsiElement element;
        if (el instanceof RsConstant) {
            type = "constant";
            element = org.rust.lang.core.psi.ext.RsConstantUtil.getNameLikeElement((RsConstant) el);
        } else if (el instanceof RsFunction) {
            type = "method";
            element = ((RsFunction) el).getIdentifier();
        } else if (el instanceof RsTypeAlias) {
            type = "type";
            element = ((RsTypeAlias) el).getIdentifier();
        } else {
            throw new IllegalStateException("unreachable");
        }

        String traitName = trait.getName();
        if (traitName == null) traitName = "";

        NavigationGutterIconBuilder<PsiElement> builder = NavigationGutterIconBuilder
            .create(icon)
            .setTargets(Collections.singletonList(superItem))
            .setTooltipText(RsBundle.message("tooltip.in", action, type, traitName));

        result.add(builder.createLineMarkerInfo(element));
    }
}
