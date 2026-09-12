/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml;
import consulo.project.Project;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.editor.gutter.GutterIconNavigationHandler;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.dataContext.DataManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.application.ApplicationManager;
import consulo.codeEditor.markup.GutterIconRenderer.Alignment;
import consulo.document.FileDocumentManager;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.language.psi.PsiElement;
import consulo.ui.event.ComponentEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import java.util.*;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.ui.image.Image;
import org.rust.openapiext.OpenApiUtil;

@ExtensionImpl
public class CargoFeatureLineMarkerProvider implements LineMarkerProvider {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.toml.lang.TomlLanguage.INSTANCE; }

    @Nullable
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@Nonnull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@Nonnull List<PsiElement> elements,
                                        @Nonnull Collection<LineMarkerInfo> result) {
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

    private boolean isFeatureKey(@Nonnull TomlKeySegment segment) {
        PsiElement keyParent = segment.getParent();
        if (keyParent == null) return false;
        PsiElement keyValueCandidate = keyParent.getParent();
        if (!(keyValueCandidate instanceof TomlKeyValue)) return false;
        PsiElement tableCandidate = keyValueCandidate.getParent();
        if (!(tableCandidate instanceof TomlTable)) return false;
        TomlTable table = (TomlTable) tableCandidate;
        return Util.isFeatureListHeader(table.getHeader());
    }

    @Nonnull
    private LineMarkerInfo<PsiElement> genFeatureLineMarkerInfo(
        @Nonnull TomlKeySegment element,
        @Nonnull String name,
        @Nullable FeatureState featureState,
        @Nonnull CargoWorkspace.Package cargoPackage
    ) {
        PsiElement anchor = element.getFirstChild();

        if (cargoPackage.getOrigin() == PackageOrigin.WORKSPACE) {
            consulo.ui.image.Image icon;
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
            consulo.ui.image.Image icon;
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

    @Nonnull
    private LineMarkerInfo<PsiElement> genSettingsLineMarkerInfo(@Nonnull TomlTableHeader header) {
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
        public void navigate(ComponentEvent<?> e, PsiElement element) {
            Context context = getContext(element);
            if (context == null) return;
            TomlKeySegment keySegment = consulo.language.psi.util.PsiTreeUtil.getParentOfType(element, TomlKeySegment.class);
            if (keySegment == null) return;
            String featureName = keySegment.getName();
            if (featureName == null) return;
            FeatureState oldState = context.myCargoPackage.getFeatureState().getOrDefault(featureName, FeatureState.Disabled);
            FeatureState newState = oldState == FeatureState.Enabled ? FeatureState.Disabled : FeatureState.Enabled;
            consulo.document.Document tomlDoc = DocumentUtil.getDocument(element.getContainingFile());
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
        public void navigate(ComponentEvent<?> e, PsiElement element) {
            Context context = getContext(element);
            if (context == null) return;
            DataContext dataContext = DataManager.getInstance().getDataContext(e.getComponent());
            createActionGroupPopup(context, dataContext).showBy(e);
        }

        @Nonnull
        private JBPopup createActionGroupPopup(@Nonnull Context context, @Nonnull DataContext dataContext) {
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

            FeaturesSettingsCheckboxAction(@Nonnull Context context, @Nonnull FeatureState newState) {
                myContext = context;
                myNewState = newState;
                String text = newState.isEnabled() ? RsBundle.message("action.enable.text") : RsBundle.message("disable");
                getTemplatePresentation().setDescription(RsBundle.message("action.all.features.description", text));
                getTemplatePresentation().setText(RsBundle.message("action.all.features.text", text));
            }

            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                myContext.myCargoProjectsService.modifyFeatures(myContext.myCargoProject, myContext.myCargoPackage.getFeatures(), myNewState);
            }
        }
    }

    private static class Context {
        public final CargoProjectsService myCargoProjectsService;
        public final CargoProject myCargoProject;
        public final CargoWorkspace.Package myCargoPackage;

        Context(@Nonnull CargoProjectsService cargoProjectsService,
                @Nonnull CargoProject cargoProject,
                @Nonnull CargoWorkspace.Package cargoPackage) {
            myCargoProjectsService = cargoProjectsService;
            myCargoProject = cargoProject;
            myCargoPackage = cargoPackage;
        }
    }

    @Nullable
    private static Context getContext(@Nonnull PsiElement element) {
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
