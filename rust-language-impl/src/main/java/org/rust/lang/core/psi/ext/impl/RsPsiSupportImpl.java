/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.PsiDirectory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.rust.lang.core.psi.RsInnerAttr;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.psi.RsVis;
import org.rust.lang.core.psi.ext.*;

import java.util.List;
import java.util.stream.Stream;

/**
 * Answers the questions the PSI contracts in {@code org.rust.lang.core.psi.ext} ask.
 * <p>
 * The interfaces there say what a Rust element is; working out the answers needs the resolve
 * engine, so each one is delegated to the helper that owns that behaviour.
 */
@Singleton
@ServiceImpl
public class RsPsiSupportImpl implements RsPsiSupport {

    @Nonnull
    @Override
    public RsAbstractableOwner owner(@Nonnull RsAbstractable element) {
        return RsAbstractableImplUtil.getOwner(element);
    }

    @Nonnull
    @Override
    public RsAbstractableOwner ownerBySyntaxOnly(@Nonnull RsAbstractable element) {
        return RsAbstractableImplUtil.getOwnerBySyntaxOnly(element);
    }

    @Nullable
    @Override
    public RsAbstractable superItem(@Nonnull RsAbstractable element) {
        return RsAbstractableImplUtil.getSuperItem(element);
    }

    @Nonnull
    @Override
    public List<RsAbstractable> searchForImplementations(@Nonnull RsAbstractable element) {
        return RsAbstractableUtil.searchForImplementations(element);
    }

    @Nonnull
    @Override
    public Stream<RsMetaItem> rawMetaItems(@Nonnull RsDocAndAttributeOwner owner) {
        return RsInnerAttributeOwnerRegistry.rawMetaItems(owner);
    }

    @Nonnull
    @Override
    public List<RsFieldDecl> fields(@Nonnull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getFields(owner);
    }

    @Nonnull
    @Override
    public List<RsInnerAttr> innerAttrs(@Nonnull RsInnerAttributeOwner owner) {
        return RsInnerAttributeOwnerRegistry.innerAttrs(owner);
    }

    @Nonnull
    @Override
    public List<RsOuterAttr> outerAttrs(@Nonnull RsOuterAttributeOwner owner) {
        return PsiElementUtil.stubChildrenOfType(owner, RsOuterAttr.class);
    }

    @Nonnull
    @Override
    public String itemKindName(@Nonnull RsItemElement element) {
        return RsItemElementUtil.getItemKindName(element);
    }

    @Nullable
    @Override
    public String referenceName(@Nonnull RsReferenceElementBase element) {
        return RsReferenceElementUtil.getReferenceName(element);
    }

    @Nullable
    @Override
    public PsiDirectory ownedDirectory(@Nonnull RsMod mod, boolean createIfNotExists) {
        return RsModUtil.getOwnedDirectory(mod, createIfNotExists);
    }

    @Nonnull
    @Override
    public List<RsMod> superMods(@Nonnull RsMod mod) {
        return RsModExtUtil.getSuperMods(mod);
    }

    @Nullable
    @Override
    public String qualifiedName(@Nonnull RsQualifiedNamedElement element) {
        return RsQualifiedNamedElementUtil.getQualifiedName(element);
    }

    @Nullable
    @Override
    public String qualifiedNameRelativeTo(@Nonnull RsQualifiedNamedElement element, @Nonnull RsMod context) {
        return RsQualifiedNamedElementUtil.qualifiedNameRelativeTo(element, context);
    }

    /** An element with no {@code pub} marker is private; otherwise the visibility the marker describes. */
    @Nonnull
    @Override
    public RsVisibility visibility(@Nonnull RsVisibilityOwner owner) {
        RsVis vis = owner.getVis();
        return vis != null ? RsVisibilityUtil.getVisibility(vis) : RsVisibility.Private.INSTANCE;
    }

    @Override
    public boolean isVisibleFrom(@Nonnull RsVisible element, @Nonnull RsMod mod) {
        return RsVisibilityUtil.isVisibleFrom(element, mod);
    }
}
