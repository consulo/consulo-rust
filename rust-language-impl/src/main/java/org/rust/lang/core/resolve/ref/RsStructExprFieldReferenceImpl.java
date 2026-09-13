/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.RsStructLiteralField;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFieldDecl;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.psi.ext.impl.RsStructLiteralFieldUtil;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve.NameResolution;

import java.util.List;

public class RsStructExprFieldReferenceImpl extends RsReferenceCached<RsStructLiteralField> {

    public RsStructExprFieldReferenceImpl(@Nonnull RsStructLiteralField field) {
        super(field);
    }

    @Nonnull
    @Override
    protected ResolveCacheDependency getCacheDependency() {
        return ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE;
    }

    @Nonnull
    @Override
    public Object[] getVariants() {
        return LookupElement.EMPTY_ARRAY;
    }

    @Nonnull
    @Override
    protected List<RsElement> resolveInner() {
        return NameResolution.collectResolveVariants(getElement().getReferenceName(), processor ->
            NameResolution.processStructLiteralFieldResolveVariants(getElement(), false, processor)
        );
    }

    @Override
    public boolean isReferenceTo(@Nonnull PsiElement target) {
        boolean canBeReferenceTo = target instanceof RsFieldDecl
            || (RsStructLiteralFieldUtil.isShorthand(getElement()) && target instanceof RsNamedElement
                && Namespace.getNamespaces((RsNamedElement) target).contains(Namespace.Values));
        return canBeReferenceTo && super.isReferenceTo(target);
    }

    @Override
    public PsiElement handleElementRename(@Nonnull String newName) {
        if (!RsStructLiteralFieldUtil.isShorthand(getElement())) {
            return super.handleElementRename(newName);
        }

        PsiElement identifier = getElement().getIdentifier();
        if (identifier == null) return getElement();

        RsPsiFactory psiFactory = new RsPsiFactory(getElement().getProject(), true, false);
        PsiElement newIdent = psiFactory.createIdentifier(newName);
        PsiElement colon = psiFactory.createColon();
        PsiElement initExpression = psiFactory.createExpression(identifier.getText());

        identifier.replace(newIdent);
        getElement().add(colon);
        getElement().add(initExpression);
        return getElement();
    }
}
