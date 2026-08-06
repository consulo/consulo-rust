/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;
import consulo.language.psi.PsiReferenceProvider;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
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
    @Nonnull
    @Override
    public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
        if (!(element instanceof RsLitExpr)) return PsiReference.EMPTY_ARRAY;
        return new PsiReference[]{new RsCfgFeatureReferenceReference((RsLitExpr) element)};
    }

    private static class RsCfgFeatureReferenceReference extends PsiPolyVariantReferenceBase<RsLitExpr> {
        public RsCfgFeatureReferenceReference(@Nonnull RsLitExpr element) {
            super(element);
        }

        @Nonnull
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
