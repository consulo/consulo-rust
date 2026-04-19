/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.RsMacroCallStub;

public abstract class RsMacroCallImplMixin extends RsStubbedElementImpl<RsMacroCallStub>
    implements RsMacroCall {

    private final SimpleModificationTracker modificationTracker = new SimpleModificationTracker();

    public RsMacroCallImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsMacroCallImplMixin(@NotNull RsMacroCallStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }

    @NotNull
    @Override
    public SimpleModificationTracker getModificationTracker() {
        return modificationTracker;
    }

    @Override
    public boolean incModificationCount(@NotNull PsiElement element) {
        modificationTracker.incModificationCount();
        boolean isStructureModification = false;
        for (PsiElement ancestor : RsElementUtil.getAncestors(this)) {
            if (ancestor instanceof RsMacroCall && "include".equals(RsMacroCallUtil.getMacroName((RsMacroCall) ancestor))) {
                isStructureModification = true;
                break;
            }
        }
        return !isStructureModification;
    }

    @Override
    public boolean isValidHost() {
        return getMacroArgument() != null;
    }

    @Override
    public PsiLanguageInjectionHost updateText(@NotNull String text) {
        RsMacroCall newMacroCall = (RsMacroCall) new RsPsiFactory(getProject(), true).createFile(text).getFirstChild();
        if (newMacroCall == null) throw new IllegalStateException(text);
        return (RsMacroCall) replace(newMacroCall);
    }

    @NotNull
    @Override
    public LiteralTextEscaper<RsMacroCall> createLiteralTextEscaper() {
        return new SimpleMultiLineTextEscaper(this);
    }
}
