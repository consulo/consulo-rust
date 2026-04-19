/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.RsLiteralKind;
import org.rust.lang.core.psi.RsLiteralKindUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlFile;

/**
 * Consider "main.rs":
 * <pre>
 * #[cfg(feature = "foo")]
 *                // Provides a reference for "foo"
 * fn foo() {}
 * </pre>
 */
public class RsCfgFeatureReferenceProvider extends PsiReferenceProvider {
    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof RsLitExpr)) return PsiReference.EMPTY_ARRAY;
        return new PsiReference[]{new RsCfgFeatureReferenceReference((RsLitExpr) element)};
    }

    private static class RsCfgFeatureReferenceReference extends PsiPolyVariantReferenceBase<RsLitExpr> {
        public RsCfgFeatureReferenceReference(@NotNull RsLitExpr element) {
            super(element);
        }

        @NotNull
        @Override
        public ResolveResult[] multiResolve(boolean incompleteCode) {
            RsLitExpr elem = getElement();
            RsLiteralKind kind = RsLiteralKindUtil.getKind(elem);
            if (!(kind instanceof RsLiteralKind.StringLiteral)) return ResolveResult.EMPTY_ARRAY;
            String literalValue = ((RsLiteralKind.StringLiteral) kind).getValue();
            if (literalValue == null) return ResolveResult.EMPTY_ARRAY;

            CargoWorkspace.Package pkg = RsElementUtil.getContainingCargoPackage(elem);
            if (pkg == null) return ResolveResult.EMPTY_ARRAY;
            TomlFile toml = Util.getPackageCargoTomlFile(pkg, elem.getProject());
            if (toml == null) return ResolveResult.EMPTY_ARRAY;
            return CargoTomlNameResolution.resolveFeature(toml, literalValue, false);
        }
    }
}
