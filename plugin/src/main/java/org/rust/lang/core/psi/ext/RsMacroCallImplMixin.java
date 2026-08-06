/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.component.util.SimpleModificationTracker;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.RsMacroCallStub;

public abstract class RsMacroCallImplMixin extends RsStubbedElementImpl<RsMacroCallStub>
    implements RsMacroCall {

    private final SimpleModificationTracker modificationTracker = new SimpleModificationTracker();

    public RsMacroCallImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsMacroCallImplMixin(@Nonnull RsMacroCallStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }

    @Nonnull
    @Override
    public SimpleModificationTracker getModificationTracker() {
        return modificationTracker;
    }

    @Override
    public boolean incModificationCount(@Nonnull PsiElement element) {
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
    public PsiLanguageInjectionHost updateText(@Nonnull String text) {
        RsMacroCall newMacroCall = (RsMacroCall) new RsPsiFactory(getProject(), true).createFile(text).getFirstChild();
        if (newMacroCall == null) throw new IllegalStateException(text);
        return (RsMacroCall) replace(newMacroCall);
    }

    @Nonnull
    @Override
    public LiteralTextEscaper<RsMacroCall> createLiteralTextEscaper() {
        return new SimpleMultiLineTextEscaper(this);
    }
}
