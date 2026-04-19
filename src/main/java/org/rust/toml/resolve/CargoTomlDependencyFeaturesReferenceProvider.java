/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlArray;
import org.toml.lang.psi.TomlFile;
import org.toml.lang.psi.TomlKeySegment;
import org.toml.lang.psi.TomlLiteral;
import org.toml.lang.psi.ext.TomlLiteralKind;
import org.toml.lang.psi.ext.TomlLiteralKt;

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
    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
        if (!(element instanceof TomlLiteral)) return PsiReference.EMPTY_ARRAY;
        return new PsiReference[]{new CargoTomlDependencyFeatureReference((TomlLiteral) element)};
    }

    private static class CargoTomlDependencyFeatureReference extends PsiPolyVariantReferenceBase<TomlLiteral> {
        public CargoTomlDependencyFeatureReference(@NotNull TomlLiteral element) {
            super(element);
        }

        @NotNull
        @Override
        public ResolveResult[] multiResolve(boolean incompleteCode) {
            TomlLiteral elem = getElement();
            Object kind = TomlLiteralKt.getKind(elem);
            if (!(kind instanceof TomlLiteralKind.String)) return ResolveResult.EMPTY_ARRAY;
            String literalValue = ((TomlLiteralKind.String) kind).getValue();
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
