/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import com.intellij.codeInspection.ProblemsHolder;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.toml.Util;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralKt;

public class CargoTomlCyclicFeatureInspection extends CargoTomlInspectionToolBase {
    @NotNull
    @Override
    protected TomlVisitor buildCargoTomlVisitor(@NotNull ProblemsHolder holder) {
        return new TomlVisitor() {
            @Override
            public void visitLiteral(@NotNull TomlLiteral element) {
                if (!(element.getParent() instanceof TomlArray)) return;
                TomlArray parentArray = (TomlArray) element.getParent();
                if (!(parentArray.getParent() instanceof TomlKeyValue)) return;
                TomlKeyValue parentKeyValue = (TomlKeyValue) parentArray.getParent();
                if (!(parentKeyValue.getParent() instanceof TomlTable)) return;
                TomlTable parentTable = (TomlTable) parentKeyValue.getParent();
                if (!Util.isFeatureListHeader(parentTable.getHeader())) return;

                String parentFeatureName = parentKeyValue.getKey().getText();
                if (parentFeatureName == null) return;
                Object kind = TomlLiteralKt.getKind(element);
                if (!(kind instanceof TomlLiteralKind.String)) return;
                String featureName = ((TomlLiteralKind.String) kind).getValue();

                if (featureName != null && featureName.equals(parentFeatureName)) {
                    holder.registerProblem(
                        element,
                        RsBundle.message("inspection.message.cyclic.feature.dependency.feature.depends.on.itself", parentFeatureName)
                    );
                }
            }
        };
    }
}
