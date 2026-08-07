/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.rust.ide.inspections.lints.RsNeedlessLifetimesInspection;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsGenericDeclaration;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.utils.RsDiagnostic;

import java.util.Collection;
import consulo.localize.LocalizeValue;
import org.rust.RsBundle;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsWrongLifetimeParametersNumberInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitPathType(@Nonnull RsPathType type) {
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
            public void visitRefLikeType(@Nonnull RsRefLikeType type) {
                if (type.getMul() == null && !ExtensionsUtil.isLifetimeElidable(type) && type.getLifetime() == null) {
                    PsiElement and = type.getAnd();
                    new RsDiagnostic.MissingLifetimeSpecifier(and != null ? and : type).addToHolder(holder);
                }
            }

            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitFunction2(@Nonnull RsFunction fn) {
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

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.wrong.lifetime.parameters.number.display.name"));
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
