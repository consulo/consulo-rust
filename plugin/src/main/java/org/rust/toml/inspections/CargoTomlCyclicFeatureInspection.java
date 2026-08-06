/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.inspections;

import consulo.language.editor.inspection.ProblemsHolder;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.toml.Util;
import org.toml.lang.psi.*;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralExt;

public class CargoTomlCyclicFeatureInspection extends CargoTomlInspectionToolBase {
    @Nonnull
    @Override
    protected TomlVisitor buildCargoTomlVisitor(@Nonnull ProblemsHolder holder) {
        return new TomlVisitor() {
            @Override
            public void visitLiteral(@Nonnull TomlLiteral element) {
                if (!(element.getParent() instanceof TomlArray)) return;
                TomlArray parentArray = (TomlArray) element.getParent();
                if (!(parentArray.getParent() instanceof TomlKeyValue)) return;
                TomlKeyValue parentKeyValue = (TomlKeyValue) parentArray.getParent();
                if (!(parentKeyValue.getParent() instanceof TomlTable)) return;
                TomlTable parentTable = (TomlTable) parentKeyValue.getParent();
                if (!Util.isFeatureListHeader(parentTable.getHeader())) return;

                String parentFeatureName = parentKeyValue.getKey().getText();
                if (parentFeatureName == null) return;
                Object kind = TomlLiteralExt.getKind(element);
                if (!(kind instanceof TomlLiteralKind.StringKind)) return;
                String featureName = ((TomlLiteralKind.StringKind) kind).getValue();

                if (featureName != null && featureName.equals(parentFeatureName)) {
                    holder.registerProblem(
                        element,
                        RsBundle.message("inspection.message.cyclic.feature.dependency.feature.depends.on.itself", parentFeatureName)
                    );
                }
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.cargo.toml.cyclic.feature.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("cargo.toml"));
    }
}
