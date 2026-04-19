/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectsUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageFeature;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.rust.lang.core.psi.ext.RsElementExtUtil;

public class ToggleFeatureIntention extends RsElementBaseIntentionAction<ToggleFeatureIntention.Context> implements HighPriorityAction {
    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.Rust.ToggleFeatureIntention.family.name");
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @NotNull
    @Override
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final String myFeatureName;
        private final RsElement myElement;

        public Context(@NotNull String featureName, @NotNull RsElement element) {
            myFeatureName = featureName;
            myElement = element;
        }

        @NotNull
        public String getFeatureName() {
            return myFeatureName;
        }

        @NotNull
        public RsElement getElement() {
            return myElement;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        List<PsiElement> expandedElementsOrSelf = RsExpandedElementUtil.findExpansionElements(element);
        if (expandedElementsOrSelf == null) {
            expandedElementsOrSelf = Collections.singletonList(element);
        }

        RsMetaItem featureMetaItem = null;
        for (PsiElement expanded : expandedElementsOrSelf) {
            RsMetaItem metaItem = PsiElementExt.ancestorOrSelf(expanded, RsMetaItem.class);
            while (metaItem != null) {
                if ("feature".equals(metaItem.getName())) {
                    PsiElement ancestor = metaItem.getParent();
                    while (ancestor != null) {
                        if (RsPsiPattern.anyCfgCondition.accepts(ancestor)) {
                            featureMetaItem = metaItem;
                            break;
                        }
                        ancestor = ancestor.getParent();
                    }
                    if (featureMetaItem != null) break;
                }
                metaItem = PsiElementExt.ancestorStrict(metaItem, RsMetaItem.class);
            }
            if (featureMetaItem != null) break;
        }
        if (featureMetaItem == null) return null;

        RsElement context = (RsElement) featureMetaItem.getLitExpr();
        if (context == null) return null;
        String featureName = org.rust.lang.core.psi.ext.RsLitExprUtil.getStringValue(featureMetaItem.getLitExpr());
        if (featureName == null) return null;
        Boolean isEnabled = isCargoFeatureEnabled(context, featureName);
        if (isEnabled == null) return null;

        if (isEnabled) {
            setText(RsBundle.message("intention.Rust.ToggleFeatureIntention.disable", featureName));
        } else {
            setText(RsBundle.message("intention.Rust.ToggleFeatureIntention.enable", featureName));
        }

        return new Context(featureName, context);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        RsElement el = ctx.getElement();
        org.rust.cargo.project.model.CargoProject cargoProject = RsElementExtUtil.getCargoProject(el);
        if (cargoProject == null) return;
        CargoWorkspace.Package pkg = RsElementUtil.getContainingCargoPackage(el);
        if (pkg == null) return;

        PackageFeature feature = null;
        for (PackageFeature f : pkg.getFeatures()) {
            if (f.getName().equals(ctx.getFeatureName())) {
                feature = f;
                break;
            }
        }
        if (feature == null) return;
        Map<String, FeatureState> featureState = pkg.getFeatureState();
        FeatureState state = featureState.get(ctx.getFeatureName());
        if (state == null) return;
        CargoProjectsUtil.getCargoProjects(project).modifyFeatures(
            cargoProject,
            Collections.singleton(feature),
            state.not()
        );
    }

    // No intention preview because it doesn't modify any code
    @NotNull
    @Override
    public IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Nullable
    private Boolean isCargoFeatureEnabled(@NotNull RsElement element, @NotNull String name) {
        CargoWorkspace.Package pkg = RsElementUtil.getContainingCargoPackage(element);
        if (pkg == null) return null;
        if (pkg.getOrigin() != PackageOrigin.WORKSPACE) return null;

        Map<String, FeatureState> featureState = pkg.getFeatureState();
        FeatureState state = featureState.get(name);
        if (state == null) return null;
        return state.isEnabled();
    }
}
