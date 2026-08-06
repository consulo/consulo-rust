/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import consulo.language.editor.inspection.ProblemsHolder;
import jakarta.annotation.Nonnull;
import org.rust.toml.crates.local.CratesLocalIndexService;
import org.toml.lang.psi.TomlValue;
import org.toml.lang.psi.TomlVisitor;

public abstract class CrateVersionInspection extends CargoTomlInspectionToolBase {
    @Override
    protected boolean requiresLocalCrateIndex() {
        return true;
    }

    protected abstract void handleCrateVersion(
        @Nonnull DependencyCrate dependency,
        @Nonnull CratesLocalIndexService.CargoRegistryCrate crate,
        @Nonnull TomlValue versionElement,
        @Nonnull ProblemsHolder holder
    );

    @Nonnull
    @Override
    protected TomlVisitor buildCargoTomlVisitor(@Nonnull ProblemsHolder holder) {
        return new CargoDependencyCrateVisitor() {
            @Override
            public void visitDependency(@Nonnull DependencyCrate dependency) {
                if (dependency.isForeign()) return;
                var result = CratesLocalIndexService.getInstance().getCrate(dependency.getCrateName());
                if (result.isErr()) return;
                CratesLocalIndexService.CargoRegistryCrate crate = result.unwrap();
                if (crate == null) return;

                TomlValue versionElement = dependency.getProperties().get("version");
                if (versionElement == null) return;
                handleCrateVersion(dependency, crate, versionElement, holder);
            }
        };
    }
}
