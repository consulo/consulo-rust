/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.refactoring.RsNamesValidatorUtil;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsPsiUtilUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsReferenceElementBase;

import java.util.List;

public abstract class RsReferenceBase<T extends RsReferenceElementBase> extends PsiPolyVariantReferenceBase<T>
    implements RsReference {

    public RsReferenceBase(@NotNull T element) {
        super(element);
    }

    @Nullable
    @Override
    public RsElement resolve() {
        PsiElement resolved = super.resolve();
        return resolved instanceof RsElement ? (RsElement) resolved : null;
    }

    @NotNull
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
    protected PsiElement getReferenceAnchor(@NotNull T element) {
        return element.getReferenceNameElement();
    }

    @NotNull
    @Override
    public final TextRange getRangeInElement() {
        return super.getRangeInElement();
    }

    @NotNull
    @Override
    protected final TextRange calculateDefaultRangeInElement() {
        PsiElement anchor = getReferenceAnchor(getElement());
        if (anchor == null) return TextRange.EMPTY_RANGE;
        return TextRange.from(anchor.getStartOffsetInParent(), anchor.getTextLength());
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newName) {
        PsiElement referenceNameElement = getElement().getReferenceNameElement();
        if (referenceNameElement != null) {
            doRename(referenceNameElement, newName);
        }
        return getElement();
    }

    @NotNull
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

    public static void doRename(@NotNull PsiElement identifier, @NotNull String newName) {
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
