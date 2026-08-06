/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemsHolder;
import io.github.z4kn4fein.semver.Version;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.toml.Util;
import org.rust.toml.crates.local.CrateVersionRequirement;
import org.rust.toml.crates.local.CratesLocalIndexService;
import org.toml.lang.psi.TomlValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NewCrateVersionAvailableInspection extends CrateVersionInspection {
    @Override
    protected void handleCrateVersion(@Nonnull DependencyCrate dependency,
                                       @Nonnull CratesLocalIndexService.CargoRegistryCrate crate,
                                       @Nonnull TomlValue versionElement,
                                       @Nonnull ProblemsHolder holder) {
        String versionText = Util.getStringValue(versionElement);
        if (versionText == null) return;
        CrateVersionRequirement versionReq = CrateVersionRequirement.build(versionText);
        if (versionReq == null) return;
        if (versionReq.isPinned()) return;

        List<Version> versions = new ArrayList<>();
        for (CratesLocalIndexService.CargoRegistryCrateVersion v : crate.getVersions()) {
            if (!v.isYanked() && v.getSemanticVersion() != null) {
                versions.add(v.getSemanticVersion());
            }
        }
        versions.sort(Collections.reverseOrder());

        Version highestMatchingVersion = null;
        for (Version v : versions) {
            if (versionReq.matches(v)) {
                highestMatchingVersion = v;
                break;
            }
        }
        if (highestMatchingVersion == null) return;

        Version newerVersion = null;
        for (Version v : versions) {
            if (isRustStable(v) && v.compareTo(highestMatchingVersion) > 0) {
                newerVersion = v;
                break;
            }
        }
        if (newerVersion == null) return;

        holder.registerProblem(
            versionElement,
            RsBundle.message("inspection.message.newer.version.available.for.crate", dependency.getCrateName(), newerVersion),
            ProblemHighlightType.WEAK_WARNING,
            new UpdateCrateVersionFix(versionElement, newerVersion.toString())
        );
    }

    private static boolean isRustStable(@Nonnull Version version) {
        return version.getPreRelease() == null;
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.new.crate.version.available.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("cargo.toml"));
    }
}
