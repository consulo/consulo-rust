/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.PsiReferenceProvider;

import consulo.document.util.TextRange;
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
import org.toml.lang.psi.TomlFile;
import org.toml.lang.psi.TomlLiteral;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralExt;

/**
 * Consider {@code Cargo.toml}:
 * <pre>
 * [features]
 * foo = []
 * bar = [ "foo" ]
 *         # Provides a reference for "foo"
 * baz = [ "some_dependency_package/feature_name" ]
 *         # and for "some_dependency_package/feature_name"
 * </pre>
 *
 * @see org.rust.toml.completion.CargoTomlFeatureDependencyCompletionProvider
 */
public class CargoTomlFeatureDependencyReferenceProvider extends PsiReferenceProvider {
    @Nonnull
    @Override
    public PsiReference[] getReferencesByElement(@Nonnull PsiElement element, @Nonnull ProcessingContext context) {
        if (!(element instanceof TomlLiteral)) return PsiReference.EMPTY_ARRAY;
        return new PsiReference[]{new CargoTomlFeatureDependencyReference((TomlLiteral) element)};
    }

    private static class CargoTomlFeatureDependencyReference extends PsiPolyVariantReferenceBase<TomlLiteral> {
        public CargoTomlFeatureDependencyReference(@Nonnull TomlLiteral element) {
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

            if (literalValue.contains("/")) {
                String[] parts = literalValue.split("/", 2);
                if (parts.length != 2) return ResolveResult.EMPTY_ARRAY;
                String firstSegment = parts[0];
                String featureName = parts[1];
                String depName = firstSegment.endsWith("?") ? firstSegment.substring(0, firstSegment.length() - 1) : firstSegment;

                TomlFile depToml = Util.findDependencyTomlFile(elem, depName);
                if (depToml == null) return ResolveResult.EMPTY_ARRAY;
                return CargoTomlNameResolution.resolveFeature(depToml, featureName, false);
            } else {
                boolean depOnly = literalValue.startsWith("dep:");
                if (!(elem.getContainingFile() instanceof TomlFile)) return ResolveResult.EMPTY_ARRAY;
                TomlFile tomlFile = (TomlFile) elem.getContainingFile();
                String featureName = literalValue.startsWith("dep:") ? literalValue.substring(4) : literalValue;
                return CargoTomlNameResolution.resolveFeature(tomlFile, featureName, depOnly);
            }
        }

        @Override
        public PsiElement handleElementRename(@Nonnull String newElementName) {
            TextRange valueRange = getRangeInElement();
            String unescapedLiteralValue = valueRange.substring(getElement().getNode().getText());
            int separatorIndex = unescapedLiteralValue.indexOf("/");
            TextRange range;
            if (separatorIndex != -1) {
                range = new TextRange(valueRange.getStartOffset() + separatorIndex + 1, valueRange.getEndOffset());
            } else {
                range = getRangeInElement();
            }
            ElementManipulator<TomlLiteral> manipulator = ElementManipulators.getManipulator(getElement());
            if (manipulator == null) return getElement();
            PsiElement result = manipulator.handleContentChange(myElement, range, newElementName);
            return result != null ? result : getElement();
        }
    }
}
