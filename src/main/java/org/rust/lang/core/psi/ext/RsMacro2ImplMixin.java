/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    public RsMacro2ImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsMacro2ImplMixin(@NotNull RsMacro2Stub stub, @NotNull IStubElementType<?, ?> elementType) {
        super(stub, elementType);
    }

    @Override
    public Icon getIcon(int flags) {
        return RsVisibilityUtil.iconWithVisibility(this, flags, RsIcons.MACRO2);
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

    @NotNull
    @Override
    public SimpleModificationTracker getModificationTracker() {
        return modificationTracker;
    }

    @Override
    public boolean incModificationCount(@NotNull PsiElement element) {
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
        return CachedValuesManager.getCachedValue(this, () -> {
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
        return CachedValuesManager.getCachedValue(this, () -> {
            String body = RsMacro2Util.prepareMacroBody(this);
            HashCode hash = HashCode.compute(body);
            return CachedValueProvider.Result.create(hash, modificationTracker);
        });
    }

    @Override
    public boolean getHasRustcBuiltinMacro() {
        return RsMacro2Util.MACRO2_HAS_RUSTC_BUILTIN_MACRO_PROP.getByPsi(this);
    }

    @NotNull
    @Override
    public MacroBraces getPreferredBraces() {
        Object stub = getStub();
        if (stub instanceof RsMacro2Stub) {
            return ((RsMacro2Stub) stub).getPreferredBraces();
        }
        return RsMacroDefinitionBaseUtil.guessPreferredBraces(this);
    }
}
