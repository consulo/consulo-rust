/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProviderDescriptor;
import consulo.util.dataholder.Key;
import consulo.application.util.NotNullLazyValue;
import consulo.language.psi.PsiElement;
import consulo.application.util.query.Query;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;
import org.rust.ide.navigation.goto_.RsGoToImplRenderer;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.QueryExtUtil;

import javax.swing.Icon;
import java.util.Collection;
import java.util.List;
import org.rust.lang.core.psi.ext.RsElement;

public class RsImplsLineMarkerProvider extends LineMarkerProviderDescriptor {

    @Nonnull
    @Override
    public consulo.language.Language getLanguage() {
        return org.rust.lang.RsLanguage.INSTANCE;
    }

    public static final Key<List<String>> RENDERED_IMPLS = Key.create("RENDERED_IMPLS");

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("gutter.rust.implemented.item.name"));
    }

    @Nonnull
    @Override
    public consulo.ui.image.Image getIcon() {
        return RsIcons.IMPLEMENTED;
    }

    @Nullable
    @Override
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@Nonnull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@Nonnull List<PsiElement> elements, @Nonnull Collection<LineMarkerInfo> result) {
        for (PsiElement el : elements) {
            Query<RsElement> query = implsQuery(el);
            if (query == null) continue;
            NotNullLazyValue<Collection<? extends PsiElement>> targets = NotNullLazyValue.createValue(() -> {
                @SuppressWarnings("unchecked")
                Collection<? extends PsiElement> found = (Collection<? extends PsiElement>) (Collection<?>) query.findAll();
                return found;
            });
            LineMarkerInfo<PsiElement> info = (LineMarkerInfo<PsiElement>) (LineMarkerInfo) ImplsGutterIconBuilder.create(el.getText(), getIcon())
                .setTargets(targets)
                .setTooltipText(RsBundle.message("gutter.rust.implemented.item.tooltip"))
                .setCellRenderer(new RsGoToImplRenderer())
                .createLineMarkerInfo(el);
            result.add(info);
        }
    }

    @Nullable
    public static Query<RsElement> implsQuery(@Nonnull PsiElement psi) {
        PsiElement parent = psi.getParent();
        if (parent instanceof RsTraitItem && ((RsTraitItem) parent).getIdentifier() == psi) {
            return QueryExtUtil.mapQuery(RsTraitItemUtil.searchForImplementations((RsTraitItem) parent), it -> it);
        }
        if (parent instanceof RsStructItem && ((RsStructItem) parent).getIdentifier() == psi) {
            return QueryExtUtil.mapQuery(RsStructOrEnumItemElementUtil.searchForImplementations((RsStructItem) parent), it -> it);
        }
        if (parent instanceof RsEnumItem && ((RsEnumItem) parent).getIdentifier() == psi) {
            return QueryExtUtil.mapQuery(RsStructOrEnumItemElementUtil.searchForImplementations((RsEnumItem) parent), it -> it);
        }
        if (parent instanceof RsAbstractable
            && RsElementUtil.getIdentifyingElement((RsAbstractable) parent) == psi
            && RsAbstractableUtil.getOwner((RsAbstractable) parent) instanceof RsAbstractableOwner.Trait) {
            java.util.List<RsAbstractable> impls = RsAbstractableUtil.searchForImplementations((RsAbstractable) parent);
            return new consulo.application.util.query.CollectionQuery<>(new java.util.ArrayList<>(impls));
        }
        return null;
    }
}
