/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.fixes.AddTokioMainFix;
import org.rust.ide.fixes.RemoveElementFix;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.utils.RsDiagnostic;

import java.util.Collections;
import java.util.List;

public class RsAsyncMainFunctionInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitFunction2(@NotNull RsFunction o) {
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
}
