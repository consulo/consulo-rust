/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.presentation.PresentationUtils;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsRawIdentifiers;

/**
 * Base class for non-stubbed named Rust PSI elements.
 * Provides name identifier lookup, getName/setName, and text offset.
 */
public abstract class RsNamedElementImpl extends RsElementImpl implements RsNameIdentifierOwner {

    public RsNamedElementImpl(@NotNull IElementType type) {
        super(type);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return findPsiChildByType(RsElementTypes.IDENTIFIER);
    }

    @Nullable
    @Override
    public String getName() {
        PsiElement nameId = getNameIdentifier();
        return nameId != null ? RsRawIdentifiers.getUnescapedText(nameId) : null;
    }

    @Override
    public PsiElement setName(@NotNull String name) {
        PsiElement nameId = getNameIdentifier();
        if (nameId != null) {
            nameId.replace(new RsPsiFactory(getProject()).createIdentifier(name));
        }
        return this;
    }

    @Override
    public int getTextOffset() {
        PsiElement nameId = getNameIdentifier();
        return nameId != null ? nameId.getTextOffset() : super.getTextOffset();
    }
}
