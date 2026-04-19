/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.imports;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.ui.popup.list.ListPopupImpl;
import com.intellij.ui.popup.list.PopupListElementRenderer;
import com.intellij.util.TextWithIcon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.RsBundle;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.icons.RsIcons;
import org.rust.ide.utils.imports.ImportCandidate;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ImportUi {

    private static ImportItemUi MOCK = null;

    private ImportUi() {}

    public static void showItemsToImportChooser(
        @NotNull Project project,
        @NotNull DataContext dataContext,
        @NotNull List<ImportCandidate> items,
        @NotNull Consumer<ImportCandidate> callback
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

    @TestOnly
    public static void withMockImportItemUi(@NotNull ImportItemUi mockUi, @NotNull Runnable action) {
        MOCK = mockUi;
        try {
            action.run();
        } finally {
            MOCK = null;
        }
    }

    public interface ImportItemUi {
        void chooseItem(@NotNull List<ImportCandidate> items, @NotNull Consumer<ImportCandidate> callback);
    }

    private static class PopupImportItemUi implements ImportItemUi {
        private final Project myProject;
        private final DataContext myDataContext;

        PopupImportItemUi(@NotNull Project project, @NotNull DataContext dataContext) {
            this.myProject = project;
            this.myDataContext = dataContext;
        }

        @Override
        public void chooseItem(@NotNull List<ImportCandidate> items, @NotNull Consumer<ImportCandidate> callback) {
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

                @NotNull
                @Override
                public String getTextFor(ImportCandidatePsiElement value) {
                    return value.myImportCandidate.getInfo().getUsePath();
                }

                @Nullable
                @Override
                public Icon getIconFor(ImportCandidatePsiElement value) {
                    return value.myImportCandidate.getItem().getIcon(0);
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
                        panel.add(psiRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus));
                        return panel;
                    };
                }
            };
            NavigationUtil.hidePopupIfDumbModeStarts(popup, myProject);
            popup.showInBestPositionFor(myDataContext);
        }
    }

    private static class ImportCandidatePsiElement extends FakePsiElement {
        final ImportCandidate myImportCandidate;

        ImportCandidatePsiElement(@NotNull ImportCandidate importCandidate) {
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

        @NotNull
        @Override
        protected Icon getIcon(@NotNull PsiElement element) {
            ImportCandidate candidate = getImportCandidate(element);
            if (candidate != null) {
                return candidate.getItem().getIcon(getIconFlags());
            }
            return super.getIcon(element);
        }

        @NotNull
        @Override
        public String getElementText(@NotNull PsiElement element) {
            ImportCandidate candidate = getImportCandidate(element);
            if (candidate != null) {
                return candidate.getItemName();
            }
            return super.getElementText(element);
        }

        @Nullable
        @Override
        public String getContainerText(@NotNull PsiElement element, @NotNull String name) {
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

        @Nullable
        @Override
        protected TextWithIcon getItemLocation(Object value) {
            ImportCandidate candidate = getImportCandidate(value);
            if (candidate == null) return null;
            Object crate = candidate.getCrate();
            if (crate == null) return null;
            PackageOrigin origin = candidate.getCrate().getOrigin();
            if (origin == PackageOrigin.STDLIB || origin == PackageOrigin.STDLIB_DEPENDENCY) {
                return new TextWithIcon(candidate.getCrate().getNormName(), RsIcons.RUST);
            } else if (origin == PackageOrigin.DEPENDENCY) {
                return new TextWithIcon(candidate.getCrate().getNormName(), CargoIcons.ICON);
            }
            return null;
        }
    }
}
