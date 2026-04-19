/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageFeature;

import java.util.Set;

public class EnableCargoFeaturesFix implements LocalQuickFix {

    private final CargoProject cargoProject;
    private final Set<PackageFeature> features;

    public EnableCargoFeaturesFix(@NotNull CargoProject cargoProject, @NotNull Set<PackageFeature> features) {
        this.cargoProject = cargoProject;
        this.features = features;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.enable.features");
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
        CargoProjectServiceUtil.getCargoProjects(project).modifyFeatures(cargoProject, features, FeatureState.Enabled);
    }
}
