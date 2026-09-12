/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.FeatureState;
import org.rust.cargo.project.workspace.PackageFeature;

import java.util.Set;
import consulo.localize.LocalizeValue;

public class EnableCargoFeaturesFix implements LocalQuickFix {

    private final CargoProject cargoProject;
    private final Set<PackageFeature> features;

    public EnableCargoFeaturesFix(@Nonnull CargoProject cargoProject, @Nonnull Set<PackageFeature> features) {
        this.cargoProject = cargoProject;
        this.features = features;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.enable.features"));
    }

    @Override
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        CargoProjectServiceUtil.getCargoProjects(project).modifyFeatures(cargoProject, features, FeatureState.Enabled);
    }
}
