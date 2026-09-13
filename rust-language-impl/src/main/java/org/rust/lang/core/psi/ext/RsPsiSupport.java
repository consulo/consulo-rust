package org.rust.lang.core.psi.ext;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.language.psi.PsiDirectory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsFieldDecl;
import org.rust.lang.core.psi.RsInnerAttr;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.RsOuterAttr;

import java.util.List;
import java.util.stream.Stream;

/**
 * The behaviour behind the PSI contracts. The interfaces in this package describe what a Rust
 * element is; the work of answering their questions needs the resolve engine, so it is done by
 * whoever implements this service rather than by the interfaces themselves.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface RsPsiSupport {
    static RsPsiSupport getInstance() {
        return Application.get().getInstance(RsPsiSupport.class);
    }

    @Nonnull
    RsAbstractableOwner owner(@Nonnull RsAbstractable element);

    @Nonnull
    RsAbstractableOwner ownerBySyntaxOnly(@Nonnull RsAbstractable element);

    @Nullable
    RsAbstractable superItem(@Nonnull RsAbstractable element);

    @Nonnull
    List<RsAbstractable> searchForImplementations(@Nonnull RsAbstractable element);

    @Nonnull
    Stream<RsMetaItem> rawMetaItems(@Nonnull RsDocAndAttributeOwner owner);

    @Nonnull
    List<RsFieldDecl> fields(@Nonnull RsFieldsOwner owner);

    @Nonnull
    List<RsInnerAttr> innerAttrs(@Nonnull RsInnerAttributeOwner owner);

    @Nonnull
    List<RsOuterAttr> outerAttrs(@Nonnull RsOuterAttributeOwner owner);

    @Nonnull
    String itemKindName(@Nonnull RsItemElement element);

    @Nullable
    String referenceName(@Nonnull RsReferenceElementBase element);

    @Nullable
    PsiDirectory ownedDirectory(@Nonnull RsMod mod, boolean createIfNotExists);

    @Nonnull
    List<RsMod> superMods(@Nonnull RsMod mod);

    @Nullable
    String qualifiedName(@Nonnull RsQualifiedNamedElement element);

    @Nullable
    String qualifiedNameRelativeTo(@Nonnull RsQualifiedNamedElement element, @Nonnull RsMod context);

    @Nonnull
    RsVisibility visibility(@Nonnull RsVisibilityOwner owner);

    boolean isVisibleFrom(@Nonnull RsVisible element, @Nonnull RsMod mod);
}
