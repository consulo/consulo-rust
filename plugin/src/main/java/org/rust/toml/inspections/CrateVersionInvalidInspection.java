/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.toml.Util;
import org.rust.toml.crates.local.CrateVersionRequirement;
import org.rust.toml.crates.local.CratesLocalIndexService;
import org.toml.lang.psi.TomlValue;

import java.util.ArrayList;
import java.util.List;

public class CrateVersionInvalidInspection extends CrateVersionInspection {
    @Override
    protected void handleCrateVersion(@Nonnull DependencyCrate dependency,
                                       @Nonnull CratesLocalIndexService.CargoRegistryCrate crate,
                                       @Nonnull TomlValue versionElement,
                                       @Nonnull ProblemsHolder holder) {
        String versionText = Util.getStringValue(versionElement);
        if (versionText == null) return;

        CrateVersionRequirement versionReq = CrateVersionRequirement.build(versionText);
        if (versionReq == null) {
            holder.registerProblem(
                versionElement,
                RsBundle.message("inspection.message.invalid.version.requirement", versionText),
                ProblemHighlightType.WEAK_WARNING
            );
            return;
        }

        List<CratesLocalIndexService.CargoRegistryCrateVersion> compatibleVersions = new ArrayList<>();
        for (CratesLocalIndexService.CargoRegistryCrateVersion v : crate.getVersions()) {
            if (v.getSemanticVersion() != null && versionReq.matches(v.getSemanticVersion())) {
                compatibleVersions.add(v);
            }
        }

        if (compatibleVersions.isEmpty()) {
            holder.registerProblem(versionElement,
                RsBundle.message("inspection.message.no.version.matching.found.for.crate", versionText, dependency.getCrateName()));
        } else {
            boolean allYanked = true;
            for (CratesLocalIndexService.CargoRegistryCrateVersion v : compatibleVersions) {
                if (!v.isYanked()) { allYanked = false; break; }
            }
            if (allYanked) {
                holder.registerProblem(versionElement,
                    RsBundle.message("inspection.message.all.versions.matching.for.crate.are.yanked", versionText, dependency.getCrateName()));
            }
        }
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.crate.version.invalid.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("cargo.toml"));
    }
}
