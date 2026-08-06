/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.ide.fixes.AddTokioMainFix;
import org.rust.ide.fixes.RemoveElementFix;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.utils.RsDiagnostic;

import java.util.Collections;
import java.util.List;

public class RsAsyncMainFunctionInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitFunction2(@Nonnull RsFunction o) {
                PsiElement async = o.getNode().findChildByType(RsElementTypes.ASYNC) != null
                    ? o.getNode().findChildByType(RsElementTypes.ASYNC).getPsi() : null;
                if (RsFunctionUtil.isMain(o) && async != null) {
                    var hardcodedProcMacros = ProcMacroAttribute.getHardcodedProcMacroAttributes(o);
                    boolean hasAsyncMainMacro = hardcodedProcMacros.stream()
                        .anyMatch(it -> it == KnownProcMacroKind.ASYNC_MAIN);
                    String entryPointName = o.getName();
                    if (!hasAsyncMainMacro && entryPointName != null) {
                        List fixes;
                        if (RsFunctionUtil.isConst(o)) {
                            fixes = Collections.emptyList();
                        } else {
                            fixes = List.of(new RemoveElementFix(async), new AddTokioMainFix(o));
                        }
                        RsDiagnostic.addToHolder(new RsDiagnostic.AsyncMainFunction(async, entryPointName, fixes), holder);
                    }
                }
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.async.main.function.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
