/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import com.intellij.codeInspection.*;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

public class MissingFeaturesInspection extends LocalInspectionTool {

    @Nullable
    @Override
    public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        if (file instanceof RsFile) {
            return checkRsFile((RsFile) file, manager, isOnTheFly);
        } else if (CargoConstants.MANIFEST_FILE.equals(file.getName())) {
            return checkCargoTomlFile(file, manager, isOnTheFly);
        }
        return null;
    }

    @Nullable
    private ProblemDescriptor[] checkCargoTomlFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
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
    private ProblemDescriptor[] checkRsFile(@NotNull RsFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
        CargoProject cargoProject = RsElementExtUtil.findCargoProject(file);
        if (cargoProject == null) return null;
        CargoWorkspace.Target target = RsElementExtUtil.getContainingCargoTarget(file);
        if (target == null) return null;
        if (target.getPkg().getOrigin() != PackageOrigin.WORKSPACE) return null;
        Set<PackageFeature> missingFeatures = collectMissingFeatureForTarget(target);
        return createProblemDescriptors(missingFeatures, manager, file, isOnTheFly, cargoProject);
    }

    @NotNull
    private Set<PackageFeature> collectMissingFeatureForTarget(@NotNull CargoWorkspace.Target target) {
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

    @NotNull
    private static Set<PackageFeature> collectMissingFeaturesForPackage(@NotNull CargoWorkspace.Package pkg) {
        Set<PackageFeature> missingFeatures = new HashSet<>();
        collectMissingFeaturesForPackage(pkg, missingFeatures);
        return missingFeatures;
    }

    private static void collectMissingFeaturesForPackage(@NotNull CargoWorkspace.Package pkg,
                                                          @NotNull Set<PackageFeature> missingFeatures) {
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

    @NotNull
    private static ProblemDescriptor[] createProblemDescriptors(
        @NotNull Set<PackageFeature> missingFeatures,
        @NotNull InspectionManager manager,
        @NotNull PsiFile file,
        boolean isOnTheFly,
        @NotNull CargoProject cargoProject
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
}
