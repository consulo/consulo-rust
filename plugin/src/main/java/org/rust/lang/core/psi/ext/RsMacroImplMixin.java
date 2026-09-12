/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.util.dataholder.Key;
import consulo.component.util.SimpleModificationTracker;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.RsMacroStub;
import org.rust.stdext.HashCode;

import javax.swing.*;
import consulo.application.util.CachedValue;
import consulo.ui.image.Image;

public abstract class RsMacroImplMixin extends RsStubbedNamedElementImpl<RsMacroStub>
    implements RsMacro {

    private static final Key<consulo.application.util.CachedValue<HashCode>> MACRO_BODY_HASH_KEY = Key.create("MACRO_BODY_HASH");

    private final SimpleModificationTracker modificationTracker = new SimpleModificationTracker();

    public RsMacroImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsMacroImplMixin(@Nonnull RsMacroStub stub, @Nonnull IStubElementType elementType) {
        super(stub, elementType);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        java.util.List<PsiElement> children = findChildrenByType(RsElementTypes.IDENTIFIER);
        return children.size() > 1 ? children.get(1) : null;
    }

    public consulo.ui.image.Image getIcon(int flags) {
        return RsIcons.MACRO;
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        String name = getName();
        return name != null ? "::" + name : null;
    }

    @Nonnull
    @Override
    public SimpleModificationTracker getModificationTracker() {
        return modificationTracker;
    }

    @Override
    public boolean incModificationCount(@Nonnull PsiElement element) {
        modificationTracker.incModificationCount();
        return false;
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }

    @Nullable
    @Override
    public RsMacroBody getMacroBodyStubbed() {
        Object stub = getStub();
        if (stub == null) return RsMacroUtil.getMacroBody(this);
        String text = ((RsMacroStub) stub).getMacroBody();
        if (text == null) return null;
        return CachedValuesManager.getManager(this.getProject()).getCachedValue(this, () ->
            CachedValueProvider.Result.create(
                new RsPsiFactory(getProject(), false).createMacroBody(text),
                modificationTracker
            )
        );
    }

    @Nullable
    @Override
    public HashCode getBodyHash() {
        RsMacroStub stub = getStub();
        if (stub != null) return stub.getBodyHash();
        return CachedValuesManager.getManager(this.getProject()).getCachedValue(this, MACRO_BODY_HASH_KEY, () -> {
            RsMacroBody body = RsMacroUtil.getMacroBody(this);
            String bodyText = body != null ? body.getText() : null;
            HashCode hash = bodyText != null ? HashCode.compute(bodyText) : null;
            return CachedValueProvider.Result.create(hash, modificationTracker);
        }, false);
    }

    @Override
    public boolean getHasRustcBuiltinMacro() {
        return RsMacroUtil.HAS_RUSTC_BUILTIN_MACRO_PROP.getByPsi(this);
    }

    @Nonnull
    @Override
    public MacroBraces getPreferredBraces() {
        Object stub = getStub();
        if (stub instanceof RsMacroStub) {
            return ((RsMacroStub) stub).getPreferredBraces();
        }
        return RsMacroDefinitionBaseUtil.guessPreferredBraces(this);
    }
}
