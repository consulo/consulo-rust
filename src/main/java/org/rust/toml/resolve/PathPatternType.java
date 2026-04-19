/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.resolve;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.toml.CargoTomlPsiPattern;

public enum PathPatternType {
    GENERAL(CargoTomlPsiPattern.INSTANCE.getPath()),
    WORKSPACE(StandardPatterns.or(CargoTomlPsiPattern.INSTANCE.getWorkspacePath(), CargoTomlPsiPattern.INSTANCE.getPackageWorkspacePath())),
    BUILD(CargoTomlPsiPattern.INSTANCE.getBuildPath());

    private final ElementPattern<? extends PsiElement> myPattern;

    PathPatternType(@NotNull ElementPattern<? extends PsiElement> pattern) {
        myPattern = pattern;
    }

    @NotNull
    public ElementPattern<? extends PsiElement> getPattern() {
        return myPattern;
    }
}
