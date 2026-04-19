/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageFeature;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.experiments.RsExperiments;
import org.rust.ide.icons.RsIcons;
import org.rust.ide.lineMarkers.RsLineMarkerInfoUtils;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementExtUtil;
import org.rust.openapiext.DocumentUtil;
import org.rust.openapiext.SaveAllDocumentsUtil;
import org.toml.lang.psi.*;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.*;

public class CargoFeatureLineMarkerProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements,
                                        @NotNull Collection<? super LineMarkerInfo<?>> result) {
        if (!Util.tomlPluginIsAbiCompatible()) return;
        if (elements.isEmpty()) return;
        PsiElement firstElement = elements.get(0);
        if (!(firstElement.getContainingFile() instanceof TomlFile)) return;
        TomlFile file = (TomlFile) firstElement.getContainingFile();
        if (!file.getName().equalsIgnoreCase(CargoConstants.MANIFEST_FILE)) return;
        CargoWorkspace.Package cargoPackage = RsElementExtUtil.findCargoPackage(file);
        if (cargoPackage == null) return;
        Map<String, FeatureState> features = cargoPackage.getFeatureState();

        for (PsiElement element : elements) {
            PsiElement parent = element.getParent();
            if (parent instanceof TomlKeySegment) {
                TomlKeySegment keySegment = (TomlKeySegment) parent;
                boolean isFeatureKey = isFeatureKey(keySegment);
                boolean isDependencyName = CargoTomlPsiPattern.INSTANCE.getOnDependencyKey().accepts(keySegment)
                    || CargoTomlPsiPattern.INSTANCE.getOnSpecificDependencyHeaderKey().accepts(keySegment);
                if (!isFeatureKey && !isDependencyName) continue;
                String featureName = keySegment.getName();
                if (featureName == null) continue;
                if (!isFeatureKey && !features.containsKey(featureName)) continue;
                result.add(genFeatureLineMarkerInfo(
                    keySegment,
                    featureName,
                    features.get(featureName),
                    cargoPackage
                ));
            }
            if (org.rust.openapiext.OpenApiUtil.isFeatureEnabled(RsExperiments.CARGO_FEATURES_SETTINGS_GUTTER)
                && RsElementUtil.getElementType(element) == TomlElementTypes.L_BRACKET
                && cargoPackage.getOrigin() == PackageOrigin.WORKSPACE) {
                if (!(parent instanceof TomlTableHeader)) continue;
                TomlTableHeader header = (TomlTableHeader) parent;
                if (!Util.isFeatureListHeader(header)) continue;
                result.add(genSettingsLineMarkerInfo(header));
            }
        }
    }

    private boolean isFeatureKey(@NotNull TomlKeySegment segment) {
        PsiElement keyParent = segment.getParent();
        if (keyParent == null) return false;
        PsiElement keyValueCandidate = keyParent.getParent();
        if (!(keyValueCandidate instanceof TomlKeyValue)) return false;
        PsiElement tableCandidate = keyValueCandidate.getParent();
        if (!(tableCandidate instanceof TomlTable)) return false;
        TomlTable table = (TomlTable) tableCandidate;
        return Util.isFeatureListHeader(table.getHeader());
    }

    @NotNull
    private LineMarkerInfo<PsiElement> genFeatureLineMarkerInfo(
        @NotNull TomlKeySegment element,
        @NotNull String name,
        @Nullable FeatureState featureState,
        @NotNull CargoWorkspace.Package cargoPackage
    ) {
        PsiElement anchor = element.getFirstChild();

        if (cargoPackage.getOrigin() == PackageOrigin.WORKSPACE) {
            Icon icon;
            if (featureState == FeatureState.Enabled) {
                icon = RsIcons.FEATURE_CHECKED_MARK;
            } else {
                icon = RsIcons.FEATURE_UNCHECKED_MARK;
            }
            return RsLineMarkerInfoUtils.create(
                anchor,
                anchor.getTextRange(),
                icon,
                ToggleFeatureAction.INSTANCE,
                Alignment.RIGHT,
                () -> "Toggle feature `" + name + "`"
            );
        } else {
            Icon icon;
            if (featureState == FeatureState.Enabled) {
                icon = RsIcons.FEATURE_CHECKED_MARK_GRAYED;
            } else {
                icon = RsIcons.FEATURE_UNCHECKED_MARK_GRAYED;
            }
            return RsLineMarkerInfoUtils.create(
                anchor,
                anchor.getTextRange(),
                icon,
                null,
                Alignment.RIGHT,
                () -> "Feature `" + name + "` is " + featureState
            );
        }
    }

    @NotNull
    private LineMarkerInfo<PsiElement> genSettingsLineMarkerInfo(@NotNull TomlTableHeader header) {
        PsiElement anchor = header.getFirstChild();

        return RsLineMarkerInfoUtils.create(
            anchor,
            anchor.getTextRange(),
            RsIcons.FEATURES_SETTINGS,
            OpenSettingsAction.INSTANCE,
            Alignment.RIGHT,
            () -> "Configure features"
        );
    }

    private static class ToggleFeatureAction implements GutterIconNavigationHandler<PsiElement> {
        public static final ToggleFeatureAction INSTANCE = new ToggleFeatureAction();

        @Override
        public void navigate(MouseEvent e, PsiElement element) {
            Context context = getContext(element);
            if (context == null) return;
            TomlKeySegment keySegment = com.intellij.psi.util.PsiTreeUtil.getParentOfType(element, TomlKeySegment.class);
            if (keySegment == null) return;
            String featureName = keySegment.getName();
            if (featureName == null) return;
            FeatureState oldState = context.myCargoPackage.getFeatureState().getOrDefault(featureName, FeatureState.Disabled);
            FeatureState newState = oldState == FeatureState.Enabled ? FeatureState.Disabled : FeatureState.Enabled;
            com.intellij.openapi.editor.Document tomlDoc = DocumentUtil.getDocument(element.getContainingFile());
            boolean isDocUnsaved = tomlDoc != null && FileDocumentManager.getInstance().isDocumentUnsaved(tomlDoc);

            if (isDocUnsaved) {
                ApplicationManager.getApplication().runWriteAction(() -> SaveAllDocumentsUtil.saveAllDocuments());
                context.myCargoProjectsService.refreshAllProjects();
            }

            context.myCargoProjectsService.modifyFeatures(
                context.myCargoProject,
                Set.of(new PackageFeature(context.myCargoPackage, featureName)),
                newState
            );
        }
    }

    private static class OpenSettingsAction implements GutterIconNavigationHandler<PsiElement> {
        public static final OpenSettingsAction INSTANCE = new OpenSettingsAction();

        @Override
        public void navigate(MouseEvent e, PsiElement element) {
            Context context = getContext(element);
            if (context == null) return;
            DataContext dataContext = DataManager.getInstance().getDataContext(e.getComponent());
            createActionGroupPopup(context, dataContext).show(new RelativePoint(e));
        }

        @NotNull
        private JBPopup createActionGroupPopup(@NotNull Context context, @NotNull DataContext dataContext) {
            List<AnAction> actions = List.of(
                new FeaturesSettingsCheckboxAction(context, FeatureState.Enabled),
                new FeaturesSettingsCheckboxAction(context, FeatureState.Disabled)
            );
            DefaultActionGroup group = new DefaultActionGroup(actions);
            return JBPopupFactory.getInstance()
                .createActionGroupPopup(null, group, dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true);
        }

        private static class FeaturesSettingsCheckboxAction extends AnAction {
            private final Context myContext;
            private final FeatureState myNewState;

            FeaturesSettingsCheckboxAction(@NotNull Context context, @NotNull FeatureState newState) {
                myContext = context;
                myNewState = newState;
                String text = newState.isEnabled() ? RsBundle.message("action.enable.text") : RsBundle.message("disable");
                getTemplatePresentation().setDescription(RsBundle.message("action.all.features.description", text));
                getTemplatePresentation().setText(RsBundle.message("action.all.features.text", text));
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                myContext.myCargoProjectsService.modifyFeatures(myContext.myCargoProject, myContext.myCargoPackage.getFeatures(), myNewState);
            }
        }
    }

    private static class Context {
        public final CargoProjectsService myCargoProjectsService;
        public final CargoProject myCargoProject;
        public final CargoWorkspace.Package myCargoPackage;

        Context(@NotNull CargoProjectsService cargoProjectsService,
                @NotNull CargoProject cargoProject,
                @NotNull CargoWorkspace.Package cargoPackage) {
            myCargoProjectsService = cargoProjectsService;
            myCargoProject = cargoProject;
            myCargoPackage = cargoPackage;
        }
    }

    @Nullable
    private static Context getContext(@NotNull PsiElement element) {
        if (!(element.getContainingFile() instanceof TomlFile)) return null;
        TomlFile file = (TomlFile) element.getContainingFile();
        if (!file.getName().equalsIgnoreCase(CargoConstants.MANIFEST_FILE)) return null;

        CargoProject cargoProject = RsElementExtUtil.findCargoProject(file);
        if (cargoProject == null) return null;
        CargoWorkspace.Package cargoPackage = RsElementExtUtil.findCargoPackage(file);
        if (cargoPackage == null) return null;
        return new Context(CargoProjectServiceUtil.getCargoProjects(file.getProject()), cargoProject, cargoPackage);
    }
}
