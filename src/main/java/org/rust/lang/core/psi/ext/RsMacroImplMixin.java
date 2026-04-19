/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
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
import org.rust.lang.core.stubs.RsMacroStub;
import org.rust.stdext.HashCode;

import javax.swing.*;

public abstract class RsMacroImplMixin extends RsStubbedNamedElementImpl<RsMacroStub>
    implements RsMacro {

    private static final Key<com.intellij.psi.util.CachedValue<HashCode>> MACRO_BODY_HASH_KEY = Key.create("MACRO_BODY_HASH");

    private final SimpleModificationTracker modificationTracker = new SimpleModificationTracker();

    public RsMacroImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsMacroImplMixin(@NotNull RsMacroStub stub, @NotNull IStubElementType<?, ?> elementType) {
        super(stub, elementType);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        java.util.List<PsiElement> children = findChildrenByType(RsElementTypes.IDENTIFIER);
        return children.size() > 1 ? children.get(1) : null;
    }

    @Override
    public Icon getIcon(int flags) {
        return RsIcons.MACRO;
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        String name = getName();
        return name != null ? "::" + name : null;
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
        Object stub = getStub();
        if (stub == null) return RsMacroUtil.getMacroBody(this);
        String text = ((RsMacroStub) stub).getMacroBody();
        if (text == null) return null;
        return CachedValuesManager.getCachedValue(this, () ->
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
        return CachedValuesManager.getCachedValue(this, MACRO_BODY_HASH_KEY, () -> {
            RsMacroBody body = RsMacroUtil.getMacroBody(this);
            String bodyText = body != null ? body.getText() : null;
            HashCode hash = bodyText != null ? HashCode.compute(bodyText) : null;
            return CachedValueProvider.Result.create(hash, modificationTracker);
        });
    }

    @Override
    public boolean getHasRustcBuiltinMacro() {
        return RsMacroUtil.HAS_RUSTC_BUILTIN_MACRO_PROP.getByPsi(this);
    }

    @NotNull
    @Override
    public MacroBraces getPreferredBraces() {
        Object stub = getStub();
        if (stub instanceof RsMacroStub) {
            return ((RsMacroStub) stub).getPreferredBraces();
        }
        return RsMacroDefinitionBaseUtil.guessPreferredBraces(this);
    }
}
