/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.PsiElement;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
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

    @TestOnly
    public static final Key<List<String>> RENDERED_IMPLS = Key.create("RENDERED_IMPLS");

    @NotNull
    @Override
    public String getName() {
        return RsBundle.message("gutter.rust.implemented.item.name");
    }

    @NotNull
    @Override
    public Icon getIcon() {
        return RsIcons.IMPLEMENTED;
    }

    @Nullable
    @Override
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        for (PsiElement el : elements) {
            Query<RsElement> query = implsQuery(el);
            if (query == null) continue;
            NotNullLazyValue<Collection<? extends PsiElement>> targets = NotNullLazyValue.createValue(() -> {
                @SuppressWarnings("unchecked")
                Collection<? extends PsiElement> found = (Collection<? extends PsiElement>) (Collection<?>) query.findAll();
                return found;
            });
            LineMarkerInfo<PsiElement> info = new ImplsGutterIconBuilder(el.getText(), getIcon())
                .setTargets(targets)
                .setTooltipText(RsBundle.message("gutter.rust.implemented.item.tooltip"))
                .setCellRenderer(() -> new RsGoToImplRenderer())
                .createLineMarkerInfo(el);
            result.add(info);
        }
    }

    @Nullable
    public static Query<RsElement> implsQuery(@NotNull PsiElement psi) {
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
            return new com.intellij.util.CollectionQuery<>(new java.util.ArrayList<>(impls));
        }
        return null;
    }
}
