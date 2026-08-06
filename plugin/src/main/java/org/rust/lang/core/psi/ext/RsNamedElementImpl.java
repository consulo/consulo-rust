/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.navigation.ItemPresentation;
import consulo.language.psi.PsiElement;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.presentation.PresentationUtils;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsRawIdentifiers;

/**
 * Base class for non-stubbed named Rust PSI elements.
 * Provides name identifier lookup, getName/setName, and text offset.
 */
public abstract class RsNamedElementImpl extends RsElementImpl implements RsNameIdentifierOwner {

    public RsNamedElementImpl(@Nonnull ASTNode node) {
        super(node);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return findChildByType(RsElementTypes.IDENTIFIER);
    }

    @Nullable
    @Override
    public String getName() {
        PsiElement nameId = getNameIdentifier();
        return nameId != null ? RsRawIdentifiers.getUnescapedText(nameId) : null;
    }

    @Override
    public PsiElement setName(@Nonnull String name) {
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
