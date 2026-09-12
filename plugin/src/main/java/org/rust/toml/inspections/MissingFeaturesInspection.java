/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageFeature;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.fixes.EnableCargoFeaturesFix;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsElementExtUtil;
import org.rust.openapiext.VirtualFileExtUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public class MissingFeaturesInspection extends LocalInspectionTool {

    @Nonnull
    @Override
    public consulo.language.editor.rawHighlight.HighlightDisplayLevel getDefaultLevel() {
        return consulo.language.editor.rawHighlight.HighlightDisplayLevel.WARNING;
    }

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
        if (file instanceof RsFile) {
            return checkRsFile((RsFile) file, manager, isOnTheFly);
        } else if (CargoConstants.MANIFEST_FILE.equals(file.getName())) {
            return checkCargoTomlFile(file, manager, isOnTheFly);
        }
        return null;
    }

    @Nullable
    private ProblemDescriptor[] checkCargoTomlFile(@Nonnull PsiFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
        CargoProject cargoProject = RsElementExtUtil.findCargoProject(file);
        if (cargoProject == null) return null;
        CargoWorkspace.Package pkg = RsElementExtUtil.findCargoPackage(file);
        if (pkg == null) return null;
        if (file.getVirtualFile() == null || file.getVirtualFile().getParent() == null) return null;
        if (!pkg.getRootDirectory().equals(VirtualFileExtUtil.pathAsPath(file.getVirtualFile().getParent()))) return null;
        Set<PackageFeature> missingFeatures = collectMissingFeaturesForPackage(pkg);
        return createProblemDescriptors(missingFeatures, manager, file, isOnTheFly, cargoProject);
    }

    @Nullable
    private ProblemDescriptor[] checkRsFile(@Nonnull RsFile file, @Nonnull InspectionManager manager, boolean isOnTheFly) {
        CargoProject cargoProject = RsElementExtUtil.findCargoProject(file);
        if (cargoProject == null) return null;
        CargoWorkspace.Target target = RsElementExtUtil.getContainingCargoTarget(file);
        if (target == null) return null;
        if (target.getPkg().getOrigin() != PackageOrigin.WORKSPACE) return null;
        Set<PackageFeature> missingFeatures = collectMissingFeatureForTarget(target);
        return createProblemDescriptors(missingFeatures, manager, file, isOnTheFly, cargoProject);
    }

    @Nonnull
    private Set<PackageFeature> collectMissingFeatureForTarget(@Nonnull CargoWorkspace.Target target) {
        Set<PackageFeature> missingFeatures = new HashSet<>();
        collectMissingFeaturesForPackage(target.getPkg(), missingFeatures);

        CargoWorkspace.Target libTarget = target.getPkg().getLibTarget();
        if (libTarget != null && !target.equals(libTarget)) {
            for (String requiredFeature : target.getRequiredFeatures()) {
                if (target.getPkg().getFeatureState().get(requiredFeature) == FeatureState.Disabled) {
                    missingFeatures.add(new PackageFeature(target.getPkg(), requiredFeature));
                }
            }
        }
        return missingFeatures;
    }

    @Nonnull
    private static Set<PackageFeature> collectMissingFeaturesForPackage(@Nonnull CargoWorkspace.Package pkg) {
        Set<PackageFeature> missingFeatures = new HashSet<>();
        collectMissingFeaturesForPackage(pkg, missingFeatures);
        return missingFeatures;
    }

    private static void collectMissingFeaturesForPackage(@Nonnull CargoWorkspace.Package pkg,
                                                          @Nonnull Set<PackageFeature> missingFeatures) {
        for (CargoWorkspace.Dependency dep : pkg.getDependencies()) {
            if (dep.getPkg().getOrigin() == PackageOrigin.WORKSPACE) {
                for (String requiredFeature : dep.getRequiredFeatures()) {
                    if (dep.getPkg().getFeatureState().get(requiredFeature) == FeatureState.Disabled) {
                        missingFeatures.add(new PackageFeature(dep.getPkg(), requiredFeature));
                    }
                }
            }
        }
    }

    @Nonnull
    private static ProblemDescriptor[] createProblemDescriptors(
        @Nonnull Set<PackageFeature> missingFeatures,
        @Nonnull InspectionManager manager,
        @Nonnull PsiFile file,
        boolean isOnTheFly,
        @Nonnull CargoProject cargoProject
    ) {
        if (missingFeatures.isEmpty()) {
            return ProblemDescriptor.EMPTY_ARRAY;
        }
        String message = RsBundle.message("inspection.message.missing.features",
            missingFeatures.stream().map(PackageFeature::toString).collect(Collectors.joining(", ")));
        return new ProblemDescriptor[]{
            manager.createProblemDescriptor(
                file,
                message,
                isOnTheFly,
                new LocalQuickFix[]{new EnableCargoFeaturesFix(cargoProject, missingFeatures)},
                ProblemHighlightType.WARNING
            )
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.missing.features.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
