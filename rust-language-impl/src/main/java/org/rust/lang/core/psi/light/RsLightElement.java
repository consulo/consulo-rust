/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.light;

import consulo.language.impl.psi.LightElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.content.scope.SearchScope;
import consulo.language.util.IncorrectOperationException;
import consulo.navigation.Navigatable;
import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.language.ast.ASTNode;

/**
 * An element of generated Rust code that stands for an element of another language - a protobuf message, for example.
 * <p>
 * The generated code is parsed - its parsed {@link #anchor} gives the element its place in the crate, its file and
 * its children - but this element is the one name resolution finds: it navigates to its {@link #origin}, and renaming
 * it renames the origin, so that the generator produces the new name.
 */
public abstract class RsLightElement<T extends RsElement> extends LightElement implements RsElement {
    @Nonnull
    protected final T anchor;
    @Nonnull
    private final PsiElement origin;

    protected RsLightElement(@Nonnull T anchor, @Nonnull PsiElement origin) {
        super(anchor.getManager(), RsLanguage.INSTANCE);
        this.anchor = anchor;
        this.origin = origin;
        RsLightElements.getInstance(anchor.getProject()).register(this);
    }

    /** The parsed generated element this one stands in for. */
    @Nonnull
    public T getAnchor() {
        return anchor;
    }

    /** The element of the source the code was generated from. */
    @Nonnull
    public PsiElement getOrigin() {
        return origin;
    }

    @Override
    public PsiFile getContainingFile() {
        return anchor.getContainingFile();
    }

    @Override
    public PsiElement getContext() {
        return anchor.getContext();
    }

    @Override
    public PsiElement getParent() {
        return anchor.getParent();
    }

    /**
     * The syntax tree of the parsed element: much of the Rust support reads a node's children directly. Going up from
     * them leads to the parsed element, not to this one.
     */
    @Override
    public ASTNode getNode() {
        return anchor.getNode();
    }

    @Override
    public PsiElement getFirstChild() {
        return anchor.getFirstChild();
    }

    @Override
    public PsiElement getLastChild() {
        return anchor.getLastChild();
    }

    @Nonnull
    @Override
    public PsiElement[] getChildren() {
        return anchor.getChildren();
    }

    @Override
    public TextRange getTextRange() {
        return anchor.getTextRange();
    }

    @Override
    public int getTextOffset() {
        return anchor.getTextOffset();
    }

    @Override
    public String getText() {
        return anchor.getText();
    }

    @Nonnull
    @Override
    public PsiElement getNavigationElement() {
        return origin.isValid() ? origin : anchor;
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (origin instanceof Navigatable navigatable && navigatable.canNavigate()) navigatable.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
        return origin instanceof Navigatable navigatable && navigatable.canNavigate();
    }

    @Override
    public boolean isValid() {
        return anchor.isValid() && origin.isValid();
    }

    /** The same generated element of another expansion of the code - each analysis builds its own - stands for the same source. */
    @Override
    public boolean isEquivalentTo(@Nullable PsiElement another) {
        return another == this || another == anchor
            || another instanceof RsLightElement<?> light && light.getClass() == getClass() && light.origin.equals(origin);
    }

    /** Writable when its source is: renaming it renames the source. */
    @Override
    public boolean isWritable() {
        return origin.isWritable();
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        return GlobalSearchScope.allScope(getProject());
    }

    @Nonnull
    @Override
    public RsMod getContainingMod() {
        return anchor.getContainingMod();
    }

    @Nullable
    @Override
    public RsMod getCrateRoot() {
        return RsElementUtil.getCrateRoot(anchor);
    }

    @Nonnull
    @Override
    public Crate getContainingCrate() {
        return anchor.getContainingCrate();
    }

    /**
     * Renames the origin: the generated code follows it. The source keeps its own spelling of the name only when the
     * new name maps back to it unchanged, which holds for the names protobuf and Rust spell alike.
     */
    @Nonnull
    protected PsiElement renameOrigin(@Nonnull String name) throws IncorrectOperationException {
        if (!(origin instanceof PsiNamedElement named)) throw new IncorrectOperationException("Cannot rename " + origin);
        named.setName(name);
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + anchor + " -> " + origin + ")";
    }
}
