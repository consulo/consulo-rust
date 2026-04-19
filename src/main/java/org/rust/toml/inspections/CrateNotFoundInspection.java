/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.toml.crates.local.CratesLocalIndexService;
import org.toml.lang.psi.TomlVisitor;

public class CrateNotFoundInspection extends CargoTomlInspectionToolBase {
    @Override
    protected boolean requiresLocalCrateIndex() {
        return true;
    }

    @NotNull
    @Override
    protected TomlVisitor buildCargoTomlVisitor(@NotNull ProblemsHolder holder) {
        return new CargoDependencyCrateVisitor() {
            @Override
            public void visitDependency(@NotNull DependencyCrate dependency) {
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
}
