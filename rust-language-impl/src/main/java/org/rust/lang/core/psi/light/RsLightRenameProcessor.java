/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.light;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.language.editor.refactoring.rename.RenamePsiElementProcessor;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nullable;

/**
 * Renaming a generated element renames the source it was generated from: the generated code follows the source, and
 * the Rust usages are renamed with the usages of the source - see {@link RsLightOriginReferencesSearcher}.
 */
@ExtensionImpl(id = "rsLightRenameProcessor", order = "before rsRenameProcessor")
public final class RsLightRenameProcessor extends RenamePsiElementProcessor {
    @Override
    public boolean canProcessElement(PsiElement element) {
        return element instanceof RsLightElement;
    }

    @Nullable
    @Override
    public PsiElement substituteElementToRename(PsiElement element, @Nullable Editor editor) {
        return element instanceof RsLightElement<?> light ? light.getOrigin() : element;
    }
}
