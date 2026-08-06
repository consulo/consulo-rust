/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.StandardPatterns;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.toml.CargoTomlPsiPattern;

public enum PathPatternType {
    GENERAL(CargoTomlPsiPattern.INSTANCE.getPath()),
    WORKSPACE(StandardPatterns.or(CargoTomlPsiPattern.INSTANCE.getWorkspacePath(), CargoTomlPsiPattern.INSTANCE.getPackageWorkspacePath())),
    BUILD(CargoTomlPsiPattern.INSTANCE.getBuildPath());

    private final ElementPattern<? extends PsiElement> myPattern;

    PathPatternType(@Nonnull ElementPattern<? extends PsiElement> pattern) {
        myPattern = pattern;
    }

    @Nonnull
    public ElementPattern<? extends PsiElement> getPattern() {
        return myPattern;
    }
}
