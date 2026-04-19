/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.checkMatch;

import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.inspections.RsLocalInspectionTool;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.ide.utils.checkMatch.CheckMatchUtil;
import org.rust.ide.utils.checkMatch.Pattern;
import org.rust.lang.core.psi.RsMatchExpr;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.utils.RsDiagnostic;

import java.util.List;

public class RsNonExhaustiveMatchInspection extends RsLocalInspectionTool {

    @NotNull
    @Override
    public String getDisplayName() {
        return RsBundle.message("non.exhaustive.match");
    }

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMatchExpr(@NotNull RsMatchExpr matchExpr) {
                List<Pattern> patterns = CheckMatchUtil.checkExhaustive(matchExpr);
                if (patterns == null) return;
                new RsDiagnostic.NonExhaustiveMatch(matchExpr, patterns).addToHolder(holder);
            }
        };
    }
}
