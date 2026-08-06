/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.doc.psi.RsDocSetextHeading;

public class RsDocSetextHeadingImpl extends RsDocElementImpl implements RsDocSetextHeading {

    public RsDocSetextHeadingImpl(@Nonnull IElementType type) {
        super(type);
    }

    @Nonnull
    @Override
    public RsMod getContainingMod() {
        RsMod mod = PsiTreeUtil.getContextOfType(
            CompletionUtilCore.getOriginalOrSelf(this), RsMod.class, true
        );
        if (mod != null) {
            return CompletionUtilCore.getOriginalOrSelf(mod);
        }
        throw new IllegalStateException("Element outside of module: " + getText());
    }
}
