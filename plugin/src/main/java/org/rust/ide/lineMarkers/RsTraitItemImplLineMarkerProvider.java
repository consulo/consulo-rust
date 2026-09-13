/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;
import org.rust.lang.RsLanguage;
import consulo.language.Language;
import consulo.language.editor.gutter.GutterIconDescriptor.Option;

import consulo.language.editor.gutter.RelatedItemLineMarkerInfo;
import consulo.language.editor.gutter.RelatedItemLineMarkerProvider;
import consulo.language.editor.ui.navigation.NavigationGutterIconBuilder;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.icons.RsIcons;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.impl.RsAbstractableUtil;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;

import consulo.ui.image.Image;
import java.util.Collection;
import java.util.Collections;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.ext.impl.RsConstantUtil;

@ExtensionImpl
public class RsTraitItemImplLineMarkerProvider extends RelatedItemLineMarkerProvider {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }

    private final Option myImplementingOption = new Option("rust.implementing.item", consulo.localize.LocalizeValue.of(RsBundle.message("gutter.rust.implementing.item")), RsIcons.IMPLEMENTING_METHOD);

    private final Option myOverridingOption = new Option("rust.overriding.item", consulo.localize.LocalizeValue.of(RsBundle.message("gutter.rust.overriding.item")), RsIcons.OVERRIDING_METHOD);

    @Override
    protected void collectNavigationMarkers(@Nonnull PsiElement el, @Nonnull Collection<? super RelatedItemLineMarkerInfo> result) {
        if (!(el instanceof RsAbstractable)) return;
        RsAbstractable abstractable = (RsAbstractable) el;

        PsiElement superItem = RsAbstractableUtil.getSuperItem(abstractable);
        if (superItem == null) return;
        RsTraitItem trait = RsElementUtil.ancestorStrict(superItem, RsTraitItem.class);
        if (trait == null) return;

        String action;
        Image icon;
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
            element = org.rust.lang.core.psi.ext.impl.RsConstantUtil.getNameLikeElement((RsConstant) el);
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
