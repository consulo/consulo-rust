/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.search;

import consulo.find.FindUsagesHandler;
import consulo.find.FindUsagesHandlerFactory;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.toml.Util;
import org.toml.lang.psi.TomlKeySegment;

public class CargoTomlFindUsagesHandlerFactory extends FindUsagesHandlerFactory {
    @Override
    public boolean canFindUsages(@Nonnull PsiElement element) {
        if (!Util.tomlPluginIsAbiCompatible()) return false;
        return element instanceof TomlKeySegment && Util.isFeatureDef((TomlKeySegment) element);
    }

    @Nullable
    @Override
    public FindUsagesHandler createFindUsagesHandler(@Nonnull PsiElement element, boolean forHighlightUsages) {
        return new FindUsagesHandler(element) {};
    }
}
