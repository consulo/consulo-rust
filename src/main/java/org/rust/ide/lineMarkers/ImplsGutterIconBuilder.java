/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.Icon;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ImplsGutterIconBuilder extends NavigationGutterIconBuilder<PsiElement> {

    private final String myElementName;

    public ImplsGutterIconBuilder(@NotNull String elementName, @NotNull Icon icon) {
        super(icon, DEFAULT_PSI_CONVERTOR, PSI_GOTO_RELATED_ITEM_PROVIDER);
        myElementName = elementName;
    }

    @NotNull
    @Override
    protected NavigationGutterIconRenderer createGutterIconRenderer(
        @NotNull NotNullLazyValue<? extends List<SmartPsiElementPointer<?>>> pointers,
        @NotNull Computable<? extends PsiElementListCellRenderer<?>> renderer,
        boolean empty,
        @Nullable GutterIconNavigationHandler<PsiElement> navigationHandler
    ) {
        return new ImplsNavigationGutterIconRenderer(
            myPopupTitle,
            myEmptyText,
            pointers,
            renderer,
            myElementName,
            myAlignment,
            myIcon,
            myTooltipText,
            empty
        );
    }

    private static class ImplsNavigationGutterIconRenderer extends NavigationGutterIconRenderer {
        private final String myElementName;
        private final Alignment myAlignment;
        private final Icon myIcon;
        @Nullable
        private final String myTooltipText;
        private final boolean myEmpty;

        ImplsNavigationGutterIconRenderer(
            @Nullable String popupTitle,
            @Nullable String emptyText,
            @NotNull NotNullLazyValue<? extends List<SmartPsiElementPointer<?>>> pointers,
            @NotNull Computable<? extends PsiElementListCellRenderer<?>> cellRenderer,
            @NotNull String elementName,
            @NotNull Alignment alignment,
            @NotNull Icon icon,
            @Nullable String tooltipText,
            boolean empty
        ) {
            super(popupTitle, emptyText, cellRenderer, pointers, !OpenApiUtil.isUnitTestMode());
            myElementName = elementName;
            myAlignment = alignment;
            myIcon = icon;
            myTooltipText = tooltipText;
            myEmpty = empty;
        }

        @Override
        public boolean isNavigateAction() {
            return !myEmpty;
        }

        @NotNull
        @Override
        public Icon getIcon() {
            return myIcon;
        }

        @Nullable
        @Override
        public String getTooltipText() {
            return myTooltipText;
        }

        @NotNull
        @Override
        public Alignment getAlignment() {
            return myAlignment;
        }

        @Override
        public void navigateToItems(@Nullable MouseEvent event) {
            if (event == null) return;

            List<? extends PsiElement> targetList = getTargetElements();
            NavigatablePsiElement[] targets = targetList.stream()
                .filter(e -> e instanceof NavigatablePsiElement)
                .map(e -> (NavigatablePsiElement) e)
                .toArray(NavigatablePsiElement[]::new);

            @SuppressWarnings({"unchecked", "rawtypes"})
            PsiElementListCellRenderer renderer = myCellRenderer.compute();
            Arrays.sort(targets, Comparator.comparing(renderer::getComparingObject));

            if (OpenApiUtil.isUnitTestMode()) {
                @SuppressWarnings("unchecked")
                List<String> renderedItems = Arrays.stream(targets)
                    .map(t -> (String) renderer.getElementText((PsiElement) t))
                    .collect(Collectors.toList());
                if (event instanceof UserDataHolder) {
                    ((UserDataHolder) event).putUserData(RsImplsLineMarkerProvider.RENDERED_IMPLS, renderedItems);
                }
            } else {
                String escapedName = StringUtil.escapeXmlEntities(myElementName);
                @SuppressWarnings("DialogTitleCapitalization")
                String chooserTitle = CodeInsightBundle.message("goto.implementation.chooserTitle", escapedName, targets.length, "");
                String findUsagesTitle = CodeInsightBundle.message("goto.implementation.findUsages.title", escapedName, targets.length);
                PsiElementListNavigator.openTargets(event, targets, chooserTitle, findUsagesTitle, renderer);
            }
        }
    }
}
