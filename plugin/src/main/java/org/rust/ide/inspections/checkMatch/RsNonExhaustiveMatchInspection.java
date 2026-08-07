/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.checkMatch;

import jakarta.annotation.Nonnull;
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
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsNonExhaustiveMatchInspection extends RsLocalInspectionTool {

    @Nonnull
@Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMatchExpr(@Nonnull RsMatchExpr matchExpr) {
                List<Pattern> patterns = CheckMatchUtil.checkExhaustive(matchExpr);
                if (patterns == null) return;
                new RsDiagnostic.NonExhaustiveMatch(matchExpr, patterns).addToHolder(holder);
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.non.exhaustive.match.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
