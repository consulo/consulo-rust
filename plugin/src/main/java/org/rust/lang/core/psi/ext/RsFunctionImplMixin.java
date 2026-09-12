/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.component.util.SimpleModificationTracker;
import consulo.language.psi.PsiElement;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.RsFunctionStub;

import javax.swing.*;
import java.util.List;
import java.util.stream.Stream;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;
import consulo.ui.image.Image;

public abstract class RsFunctionImplMixin extends RsStubbedNamedElementImpl<RsFunctionStub>
    implements RsFunction, RsModificationTrackerOwner {

    private final SimpleModificationTracker modificationTracker = new SimpleModificationTracker();

    public RsFunctionImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsFunctionImplMixin(@Nonnull RsFunctionStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public boolean isAbstract() {
        return getStub() != null ? getStub().isAbstract() : RsFunctionUtil.getBlock(this) == null;
    }

    @Override
    public boolean isUnsafe() {
        return getStub() != null ? getStub().isUnsafe() : getUnsafe() != null;
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

    public consulo.ui.image.Image getIcon(int flags) {
        return getIcon(flags, true);
    }

    @Override
    public consulo.ui.image.Image getIcon(int flags, boolean allowNameResolution) {
        RsAbstractableOwner owner = allowNameResolution ? RsAbstractableUtil.getOwner(this) : RsAbstractableUtil.getOwnerBySyntaxOnly(this);
        if (owner == RsAbstractableOwner.Free || owner == RsAbstractableOwner.Foreign) {
            if (allowNameResolution && RsFunctionUtil.isTest(this)) return RsIcons.addTestMark(RsIcons.FUNCTION);
            if (allowNameResolution && RsFunctionUtil.isProcMacroDef(this)) return RsIcons.PROC_MACRO;
            return RsIcons.FUNCTION;
        }
        // Trait or Impl
        consulo.ui.image.Image icon;
        if (RsFunctionUtil.isAssocFn(this) && isAbstract()) icon = RsIcons.ABSTRACT_ASSOC_FUNCTION;
        else if (RsFunctionUtil.isAssocFn(this)) icon = RsIcons.ASSOC_FUNCTION;
        else if (isAbstract()) icon = RsIcons.ABSTRACT_METHOD;
        else icon = RsIcons.METHOD;
        if (!owner.isInherentImpl()) return icon;
        return RsVisibilityUtil.iconWithVisibility(this, flags, icon);
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
        RsBlock block = RsFunctionUtil.getBlock(this);
        boolean shouldInc = block != null && PsiTreeUtil.isAncestor(block, element, false)
            && PsiTreeUtil.findChildOfAnyType(element, false, RsItemElement.class, RsMacro.class) == null;
        if (shouldInc) modificationTracker.incModificationCount();
        return shouldInc;
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        PsiElement derive = getDeriveProcMacroNameIdentifier();
        return derive != null ? derive : super.getNameIdentifier();
    }

    @Nullable
    private PsiElement getDeriveProcMacroNameIdentifier() {
        RsFunctionStub stub = getStub();
        if (stub != null && !stub.getMayBeProcMacroDef()) return null;

        RsMetaItem singleDerive = null;
        for (RsMetaItem meta : (Iterable<RsMetaItem>) () -> getRawOuterMetaItems().iterator()) {
            if (canBeAttrProcMacro(meta)) return null;
            if ("proc_macro_derive".equals(RsMetaItemUtil.getName(meta))) {
                if (singleDerive != null) return null;
                singleDerive = meta;
            }
        }
        if (singleDerive == null) return null;
        if (singleDerive.getMetaItemArgs() == null) return null;
        List<RsMetaItem> list = singleDerive.getMetaItemArgs().getMetaItemList();
        if (list.isEmpty()) return null;
        RsPath path = list.get(0).getPath();
        return path != null ? path.getIdentifier() : null;
    }

    private static boolean canBeAttrProcMacro(@Nonnull RsMetaItem meta) {
        return "cfg_attr".equals(RsMetaItemUtil.getName(meta))
            || RsProcMacroPsiUtil.canBeProcMacroAttributeCallWithoutContextCheck(meta, CustomAttributes.EMPTY);
    }

    @Nullable
    public String getFunctionName() {
        return RsFunctionUtil.isProcMacroDef(this) ? (super.getNameIdentifier() != null ? super.getNameIdentifier().getText() : null) : getName();
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
