/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.doc.psi.RsDocLinkText;

public class RsDocLinkTextImpl extends RsDocElementImpl implements RsDocLinkText {

    public RsDocLinkTextImpl(@NotNull IElementType type) {
        super(type);
    }

    @NotNull
    @Override
    public RsMod getContainingMod() {
        RsMod mod = PsiTreeUtil.getContextOfType(
            CompletionUtil.getOriginalOrSelf(this), RsMod.class, true
        );
        if (mod != null) {
            return CompletionUtil.getOriginalOrSelf(mod);
        }
        throw new IllegalStateException("Element outside of module: " + getText());
    }
}
