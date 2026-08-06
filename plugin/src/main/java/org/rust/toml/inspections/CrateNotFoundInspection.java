/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import consulo.language.editor.inspection.ProblemsHolder;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.toml.crates.local.CratesLocalIndexService;
import org.toml.lang.psi.TomlVisitor;

public class CrateNotFoundInspection extends CargoTomlInspectionToolBase {
    @Override
    protected boolean requiresLocalCrateIndex() {
        return true;
    }

    @Nonnull
    @Override
    protected TomlVisitor buildCargoTomlVisitor(@Nonnull ProblemsHolder holder) {
        return new CargoDependencyCrateVisitor() {
            @Override
            public void visitDependency(@Nonnull DependencyCrate dependency) {
                if (dependency.isForeign()) return;
                String crateName = dependency.getCrateName();
                var result = CratesLocalIndexService.getInstance().getCrate(crateName);
                if (result.isErr()) return;
                CratesLocalIndexService.CargoRegistryCrate crate = result.unwrap();

                if (crate == null) {
                    holder.registerProblem(dependency.getCrateNameElement(),
                        RsBundle.message("inspection.message.crate.not.found", crateName));
                }
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.crate.not.found.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("cargo.toml"));
    }
}
