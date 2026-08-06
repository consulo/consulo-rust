/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
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
import org.rust.lang.core.stubs.RsMacro2Stub;
import org.rust.stdext.HashCode;

import javax.swing.*;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

public abstract class RsMacro2ImplMixin extends RsStubbedNamedElementImpl<RsMacro2Stub>
    implements RsMacro2 {

    private final SimpleModificationTracker modificationTracker = new SimpleModificationTracker();

    public RsMacro2ImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsMacro2ImplMixin(@Nonnull RsMacro2Stub stub, @Nonnull IStubElementType elementType) {
        super(stub, elementType);
    }

    public consulo.ui.image.Image getIcon(int flags) {
        return RsVisibilityUtil.iconWithVisibility(this, flags, RsIcons.MACRO2);
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
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
        return CachedValuesManager.getManager(this.getProject()).getCachedValue(this, () -> {
            Object stub = getStub();
            String text = stub instanceof RsMacro2Stub ? ((RsMacro2Stub) stub).getMacroBody() : RsMacro2Util.prepareMacroBody(this);
            return CachedValueProvider.Result.create(
                new RsPsiFactory(getProject(), false).createMacroBody(text),
                modificationTracker
            );
        });
    }

    @Nullable
    @Override
    public HashCode getBodyHash() {
        RsMacro2Stub stub = getStub();
        if (stub != null) return stub.getBodyHash();
        return CachedValuesManager.getManager(this.getProject()).getCachedValue(this, () -> {
            String body = RsMacro2Util.prepareMacroBody(this);
            HashCode hash = HashCode.compute(body);
            return CachedValueProvider.Result.create(hash, modificationTracker);
        });
    }

    @Override
    public boolean getHasRustcBuiltinMacro() {
        return RsMacro2Util.MACRO2_HAS_RUSTC_BUILTIN_MACRO_PROP.getByPsi(this);
    }

    @Nonnull
    @Override
    public MacroBraces getPreferredBraces() {
        Object stub = getStub();
        if (stub instanceof RsMacro2Stub) {
            return ((RsMacro2Stub) stub).getPreferredBraces();
        }
        return RsMacroDefinitionBaseUtil.guessPreferredBraces(this);
    }
}
