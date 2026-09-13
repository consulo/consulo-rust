/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.imports;

import consulo.navigation.NavigationUtil;
import consulo.language.editor.ui.DefaultPsiElementCellRenderer;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.language.psi.PsiElement;
import consulo.language.impl.psi.FakePsiElement;
import consulo.ide.impl.idea.ui.popup.list.ListPopupImpl;
import consulo.ui.ex.awt.popup.PopupListElementRenderer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.RsBundle;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.icons.RsIcons;
import org.rust.lang.core.imports.ImportCandidate;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import consulo.language.editor.ui.PopupNavigationUtil;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.ui.image.Image;

public final class ImportUi {

    private static ImportItemUi MOCK = null;

    private ImportUi() {}

    public static void showItemsToImportChooser(
        @Nonnull Project project,
        @Nonnull DataContext dataContext,
        @Nonnull List<ImportCandidate> items,
        @Nonnull Consumer<ImportCandidate> callback
    ) {
        ImportItemUi itemImportUi;
        if (OpenApiUtil.isUnitTestMode()) {
            if (MOCK == null) {
                List<String> paths = items.stream().map(i -> i.getInfo().getUsePath()).collect(Collectors.toList());
                throw new IllegalStateException("Multiple items: " + paths + ". You should set mock ui via `withMockImportItemUi`");
            }
            itemImportUi = MOCK;
        } else {
            itemImportUi = new PopupImportItemUi(project, dataContext);
        }
        itemImportUi.chooseItem(items, callback);
    }

    
    public static void withMockImportItemUi(@Nonnull ImportItemUi mockUi, @Nonnull Runnable action) {
        MOCK = mockUi;
        try {
            action.run();
        } finally {
            MOCK = null;
        }
    }

    public interface ImportItemUi {
        void chooseItem(@Nonnull List<ImportCandidate> items, @Nonnull Consumer<ImportCandidate> callback);
    }

    private static class PopupImportItemUi implements ImportItemUi {
        private final Project myProject;
        private final DataContext myDataContext;

        PopupImportItemUi(@Nonnull Project project, @Nonnull DataContext dataContext) {
            this.myProject = project;
            this.myDataContext = dataContext;
        }

        @Override
        public void chooseItem(@Nonnull List<ImportCandidate> items, @Nonnull Consumer<ImportCandidate> callback) {
            List<ImportCandidatePsiElement> candidatePsiItems = new ArrayList<>();
            for (ImportCandidate item : items) {
                candidatePsiItems.add(new ImportCandidatePsiElement(item));
            }

            BaseListPopupStep<ImportCandidatePsiElement> step = new BaseListPopupStep<ImportCandidatePsiElement>(
                RsBundle.message("popup.title.item.to.import"), candidatePsiItems
            ) {
                @Override
                public boolean isAutoSelectionEnabled() {
                    return false;
                }

                @Override
                public boolean isSpeedSearchEnabled() {
                    return true;
                }

                @Override
                public boolean hasSubstep(ImportCandidatePsiElement selectedValue) {
                    return false;
                }

                @Nullable
                @Override
                public PopupStep<?> onChosen(ImportCandidatePsiElement selectedValue, boolean finalChoice) {
                    if (selectedValue == null) return PopupStep.FINAL_CHOICE;
                    return doFinalStep(() -> callback.accept(selectedValue.myImportCandidate));
                }

                @Nonnull
                @Override
                public String getTextFor(ImportCandidatePsiElement value) {
                    return value.myImportCandidate.getInfo().getUsePath();
                }

                @Nullable
                @Override
                public consulo.ui.image.Image getIconFor(ImportCandidatePsiElement value) {
                    return consulo.language.icon.IconDescriptorUpdaters.getIcon(value.myImportCandidate.getItem(), 0);
                }
            };

            ListPopupImpl popup = new ListPopupImpl(myProject, step) {
                @Override
                protected ListCellRenderer<?> getListElementRenderer() {
                    @SuppressWarnings("unchecked")
                    PopupListElementRenderer<Object> baseRenderer = (PopupListElementRenderer<Object>) super.getListElementRenderer();
                    RsImportCandidateCellRenderer psiRenderer = new RsImportCandidateCellRenderer();
                    return (list, value, index, isSelected, cellHasFocus) -> {
                        JPanel panel = new JPanel(new BorderLayout());
                        baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                        panel.add(baseRenderer.getNextStepLabel(), BorderLayout.EAST);
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        Component psiComp = psiRenderer.getListCellRendererComponent((JList) list, (PsiElement) value, index, isSelected, cellHasFocus);
                        panel.add(psiComp);
                        return panel;
                    };
                }
            };
            consulo.language.editor.ui.PopupNavigationUtil.hidePopupIfDumbModeStarts(popup, myProject);
            popup.showInBestPositionFor(myDataContext);
        }
    }

    private static class ImportCandidatePsiElement extends FakePsiElement {
        final ImportCandidate myImportCandidate;

        ImportCandidatePsiElement(@Nonnull ImportCandidate importCandidate) {
            this.myImportCandidate = importCandidate;
        }

        @Nullable
        @Override
        public PsiElement getParent() {
            return myImportCandidate.getItem().getParent();
        }
    }

    private static class RsImportCandidateCellRenderer extends DefaultPsiElementCellRenderer {

        @Nullable
        private static ImportCandidate getImportCandidate(@Nullable Object value) {
            if (value instanceof ImportCandidatePsiElement) {
                return ((ImportCandidatePsiElement) value).myImportCandidate;
            }
            return null;
        }

        @Nonnull
        @Override
        protected consulo.ui.image.Image getIcon(@Nonnull PsiElement element) {
            ImportCandidate candidate = getImportCandidate(element);
            if (candidate != null) {
                return consulo.language.icon.IconDescriptorUpdaters.getIcon(candidate.getItem(), getIconFlags());
            }
            return super.getIcon(element);
        }

        @Nonnull
        @Override
        public String getElementText(@Nonnull PsiElement element) {
            ImportCandidate candidate = getImportCandidate(element);
            if (candidate != null) {
                return candidate.getItemName();
            }
            return super.getElementText(element);
        }

        @Nullable
        @Override
        public String getContainerText(@Nonnull PsiElement element, @Nonnull String name) {
            ImportCandidate candidate = getImportCandidate(element);
            if (candidate != null) {
                List<String> path = java.util.Arrays.asList(candidate.getPath());
                String container;
                if (path.size() == 1) {
                    container = path.get(0);
                } else {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < path.size() - 1; i++) {
                        if (i > 0) sb.append("::");
                        sb.append(path.get(i));
                    }
                    container = sb.toString();
                    if (container.startsWith("crate::")) {
                        container = container.substring("crate::".length());
                    }
                }
                return "(" + container + ")";
            }
            return super.getContainerText(element, name);
        }

    }
}
