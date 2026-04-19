/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.lints.RsNeedlessLifetimesInspection;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsGenericDeclaration;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.utils.RsDiagnostic;

import java.util.Collection;

public class RsWrongLifetimeParametersNumberInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitPathType(@NotNull RsPathType type) {
                RsPath path = type.getPath();

                // Don't apply generic declaration checks to Fn-traits and `Self`
                if (path.getValueParameterList() != null) return;
                if (path.getCself() != null) return;

                PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
                if (!(resolved instanceof RsGenericDeclaration)) return;
                RsGenericDeclaration paramsDecl = (RsGenericDeclaration) resolved;
                int expectedLifetimes = RsGenericDeclarationUtil.getLifetimeParameters(paramsDecl).size();
                int actualLifetimes = RsPathUtil.getLifetimeArguments(path).size();
                if (expectedLifetimes == actualLifetimes) return;
                if (actualLifetimes == 0 && !ExtensionsUtil.isLifetimeElidable(type)) {
                    new RsDiagnostic.MissingLifetimeSpecifier(type).addToHolder(holder);
                } else if (actualLifetimes > 0) {
                    new RsDiagnostic.WrongNumberOfLifetimeArguments(type, expectedLifetimes, actualLifetimes)
                        .addToHolder(holder);
                }
            }

            @Override
            public void visitRefLikeType(@NotNull RsRefLikeType type) {
                if (type.getMul() == null && !ExtensionsUtil.isLifetimeElidable(type) && type.getLifetime() == null) {
                    PsiElement and = type.getAnd();
                    new RsDiagnostic.MissingLifetimeSpecifier(and != null ? and : type).addToHolder(holder);
                }
            }

            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitFunction2(@NotNull RsFunction fn) {
                // https://doc.rust-lang.org/book/ch10-03-lifetime-syntax.html#lifetime-elision
                if (!RsNeedlessLifetimesInspection.hasMissingLifetimes(fn)) return;

                RsRetType retType = fn.getRetType();
                if (retType == null) return;

                // Skipping `Fn(...) -> ...` and `fn(...) -> ...`
                Collection<PsiElement> descendants = PsiTreeUtil.findChildrenOfType(retType, PsiElement.class);
                for (PsiElement descendant : descendants) {
                    if (descendant instanceof RsFnPointerType) continue;
                    if (descendant instanceof RsPath && ((RsPath) descendant).getValueParameterList() != null) continue;
                    if (descendant instanceof RsRefLikeType) {
                        RsRefLikeType refLikeType = (RsRefLikeType) descendant;
                        PsiElement and = refLikeType.getAnd();
                        if (and == null) continue;
                        if (refLikeType.getLifetime() != null) continue;
                        new RsDiagnostic.MissingLifetimeSpecifier(and).addToHolder(holder);
                    }
                }
            }
        };
    }
}
