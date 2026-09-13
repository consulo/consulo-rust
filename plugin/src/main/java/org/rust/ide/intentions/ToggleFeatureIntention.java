/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.language.editor.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.model.CargoProjectsUtil;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.FeatureState;
import org.rust.cargo.api.workspace.PackageFeature;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.ext.impl.PsiElementExt;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.rust.lang.core.psi.ext.impl.RsElementExtUtil;
import consulo.localize.LocalizeValue;
import org.rust.cargo.api.model.CargoProject;
import org.rust.lang.core.psi.ext.impl.RsLitExprUtil;

public class ToggleFeatureIntention extends RsElementBaseIntentionAction<ToggleFeatureIntention.Context> implements HighPriorityAction {
    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.Rust.ToggleFeatureIntention.family.name"));
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nonnull
    @Override
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final String myFeatureName;
        private final RsElement myElement;

        public Context(@Nonnull String featureName, @Nonnull RsElement element) {
            myFeatureName = featureName;
            myElement = element;
        }

        @Nonnull
        public String getFeatureName() {
            return myFeatureName;
        }

        @Nonnull
        public RsElement getElement() {
            return myElement;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
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
        String featureName = org.rust.lang.core.psi.ext.impl.RsLitExprUtil.getStringValue(featureMetaItem.getLitExpr());
        if (featureName == null) return null;
        Boolean isEnabled = isCargoFeatureEnabled(context, featureName);
        if (isEnabled == null) return null;

        if (isEnabled) {
            setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.Rust.ToggleFeatureIntention.disable", featureName)));
        } else {
            setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.Rust.ToggleFeatureIntention.enable", featureName)));
        }

        return new Context(featureName, context);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        RsElement el = ctx.getElement();
        org.rust.cargo.api.model.CargoProject cargoProject = RsElementExtUtil.getCargoProject(el);
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
    @Nonnull
    public IntentionPreviewInfo generatePreview(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return IntentionPreviewInfo.EMPTY;
    }

    @Nullable
    private Boolean isCargoFeatureEnabled(@Nonnull RsElement element, @Nonnull String name) {
        CargoWorkspace.Package pkg = RsElementUtil.getContainingCargoPackage(element);
        if (pkg == null) return null;
        if (pkg.getOrigin() != PackageOrigin.WORKSPACE) return null;

        Map<String, FeatureState> featureState = pkg.getFeatureState();
        FeatureState state = featureState.get(name);
        if (state == null) return null;
        return state.isEnabled();
    }
}
