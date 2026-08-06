/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.PsiReferenceProvider;

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
import org.rust.toml.Util;
import org.toml.lang.psi.TomlArray;
import org.toml.lang.psi.TomlFile;
import org.toml.lang.psi.TomlKeySegment;
import org.toml.lang.psi.TomlLiteral;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralExt;

/**
 * Consider {@code Cargo.toml}:
 * <pre>
 * [dependencies]
 * foo = { version = "*", features = ["bar"] }
 *                                    # Provides a reference for "bar"
 *
 * [dependencies.foo]
 * features = ["baz"]
 *             # Provides a reference for "baz"
 * </pre>
 *
 * @see org.rust.toml.completion.CargoTomlDependencyFeaturesCompletionProvider
 */
public class CargoTomlDependencyFeaturesReferenceProvider extends PsiReferenceProvider {
    @Nonnull
    @Override
    public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
        if (!(element instanceof TomlLiteral)) return PsiReference.EMPTY_ARRAY;
        return new PsiReference[]{new CargoTomlDependencyFeatureReference((TomlLiteral) element)};
    }

    private static class CargoTomlDependencyFeatureReference extends PsiPolyVariantReferenceBase<TomlLiteral> {
        public CargoTomlDependencyFeatureReference(@Nonnull TomlLiteral element) {
            super(element);
        }

        @Nonnull
        @Override
        public ResolveResult[] multiResolve(boolean incompleteCode) {
            TomlLiteral elem = getElement();
            Object kind = TomlLiteralExt.getKind(elem);
            if (!(kind instanceof TomlLiteralKind.StringKind)) return ResolveResult.EMPTY_ARRAY;
            String literalValue = ((TomlLiteralKind.StringKind) kind).getValue();
            if (literalValue == null) return ResolveResult.EMPTY_ARRAY;

            if (!(elem.getParent() instanceof TomlArray)) return ResolveResult.EMPTY_ARRAY;
            TomlArray parentArray = (TomlArray) elem.getParent();
            TomlKeySegment pkgNameSegment = Util.getContainingDependencyKey(parentArray);
            if (pkgNameSegment == null) return ResolveResult.EMPTY_ARRAY;
            String pkgName = pkgNameSegment.getText();

            TomlFile depToml = Util.findDependencyTomlFile(elem, pkgName);
            if (depToml == null) return ResolveResult.EMPTY_ARRAY;
            return CargoTomlNameResolution.resolveFeature(depToml, literalValue, false);
        }
    }
}
