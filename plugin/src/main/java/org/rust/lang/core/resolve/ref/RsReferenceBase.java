/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.PsiPolyVariantReferenceBase;
import consulo.language.psi.ResolveResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.refactoring.RsNamesValidatorUtil;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsPsiUtilUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsReferenceElementBase;

import java.util.List;

public abstract class RsReferenceBase<T extends RsReferenceElementBase> extends PsiPolyVariantReferenceBase<T>
    implements RsReference {

    public RsReferenceBase(@Nonnull T element) {
        super(element);
    }

    @Nullable
    @Override
    public RsElement resolve() {
        PsiElement resolved = super.resolve();
        return resolved instanceof RsElement ? (RsElement) resolved : null;
    }

    @Nonnull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<RsElement> elements = multiResolve();
        ResolveResult[] results = new ResolveResult[elements.size()];
        for (int i = 0; i < elements.size(); i++) {
            results[i] = new PsiElementResolveResult(elements.get(i));
        }
        return results;
    }

    @Nullable
    protected PsiElement getReferenceAnchor(@Nonnull T element) {
        return element.getReferenceNameElement();
    }

    @Nonnull
    @Override
    public final TextRange getRangeInElement() {
        return super.getRangeInElement();
    }

    @Nonnull
    @Override
    protected final TextRange calculateDefaultRangeInElement() {
        PsiElement anchor = getReferenceAnchor(getElement());
        if (anchor == null) return TextRange.EMPTY_RANGE;
        return TextRange.from(anchor.getStartOffsetInParent(), anchor.getTextLength());
    }

    @Override
    public PsiElement handleElementRename(@Nonnull String newName) {
        PsiElement referenceNameElement = getElement().getReferenceNameElement();
        if (referenceNameElement != null) {
            doRename(referenceNameElement, newName);
        }
        return getElement();
    }

    @Nonnull
    @Override
    public Object[] getVariants() {
        return LookupElement.EMPTY_ARRAY;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RsReferenceBase && getElement() == ((RsReferenceBase<?>) other).getElement();
    }

    @Override
    public int hashCode() {
        return getElement().hashCode();
    }

    public static void doRename(@Nonnull PsiElement identifier, @Nonnull String newName) {
        RsPsiFactory factory = new RsPsiFactory(identifier.getProject());
        PsiElement newId;
        if (identifier.getNode().getElementType() == RsElementTypes.IDENTIFIER) {
            String name = RsPsiUtilUtil.escapeIdentifierIfNeeded(newName.replace(".rs", ""));
            if (!RsNamesValidatorUtil.isValidRustVariableIdentifier(name)) return;
            newId = factory.createIdentifier(name);
        } else if (identifier.getNode().getElementType() == RsElementTypes.QUOTE_IDENTIFIER) {
            newId = factory.createQuoteIdentifier(newName);
        } else if (identifier.getNode().getElementType() == RsElementTypes.META_VAR_IDENTIFIER) {
            newId = factory.createMetavarIdentifier(newName);
        } else {
            throw new IllegalStateException("Unsupported identifier type for `" + newName + "` (" + identifier.getNode().getElementType() + ")");
        }
        identifier.replace(newId);
    }
}
