/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
// ElementBase isn't exposed in Consulo's language-impl
import consulo.platform.base.icon.PlatformIconGroup;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;

import javax.swing.*;
import org.rust.lang.core.psi.ext.impl.RsTraitRefUtil;
import consulo.language.psi.PsiElement;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.rust.lang.core.stubs.RsVisStub;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.*;

public final class RsVisibilityUtil {
    private RsVisibilityUtil() {
    }

    @Nonnull
    public static consulo.ui.image.Image iconWithVisibility(@Nonnull RsVisibilityOwner owner, int flags, @Nonnull consulo.ui.image.Image icon) {
        RsVis vis = owner.getVis();
        consulo.ui.image.Image visibilityIcon;
        RsVisStubKind stubKind = vis != null ? getStubKind(vis) : null;
        if (stubKind == RsVisStubKind.PUB) {
            visibilityIcon = PlatformIconGroup.nodesC_public();
        } else if (stubKind == RsVisStubKind.CRATE || stubKind == RsVisStubKind.RESTRICTED) {
            visibilityIcon = PlatformIconGroup.nodesC_protected();
        } else {
            visibilityIcon = PlatformIconGroup.nodesC_private();
        }
        return consulo.ui.image.ImageEffects.layered(icon, visibilityIcon);
    }

    public static boolean isVisibleFrom(@Nonnull RsVisible element, @Nonnull RsMod mod) {
        // XXX: this hack fixes false-positive "E0603 module is private" for modules with multiple
        // declarations. It produces false-negatives.
        if (element instanceof RsFile && ((RsFile) element).getDeclarations().size() > 1) return true;

        RsVisibility visibility = element.getVisibility();
        if (visibility instanceof RsVisibility.Public) return true;

        RsMod elementMod;
        if (visibility instanceof RsVisibility.Private) {
            if (element instanceof RsMod) {
                elementMod = ((RsMod) element).getSuper();
            } else {
                elementMod = element.getContainingMod();
            }
            if (elementMod == null) return true;
        } else {
            elementMod = ((RsVisibility.Restricted) visibility).getInMod();
        }

        // We have access to any item in any super module of `mod`
        if (mod.getSuperMods().contains(elementMod)) return true;
        if (mod instanceof RsFile && ((RsFile) mod).getOriginalFile() == elementMod) return true;

        // Enum variants in a pub enum are public by default
        if (element instanceof RsNamedFieldDecl
            && element.getParent() != null
            && element.getParent().getParent() instanceof RsEnumVariant) {
            return true;
        }

        consulo.language.psi.PsiElement context = element.getContext();
        if (!(context instanceof RsMembers)) return false;
        consulo.language.psi.PsiElement parent = context.getContext();
        if (parent == null) return true;

        if (parent instanceof RsImplItem && ((RsImplItem) parent).getTraitRef() != null) {
            RsTraitItem resolved = RsTraitRefUtil.resolveToTrait(((RsImplItem) parent).getTraitRef());
            return resolved == null || isVisibleFrom(resolved, mod);
        }
        if (parent instanceof RsTraitItem) {
            return isVisibleFrom((RsTraitItem) parent, mod);
        }
        return false;
    }

    /**
     * If some field of a struct/enum is private (not visible from [mod]),
     * it isn't possible to instantiate it at [mod] anyhow.
     */
    public static boolean canBeInstantiatedIn(@Nonnull RsFieldsOwner owner, @Nonnull RsMod mod) {
        return RsFieldsOwnerExtUtil.canBeInstantiatedIn(owner, mod);
    }

    @Nonnull
    public static RsVisibility intersect(@Nonnull RsVisibility a, @Nonnull RsVisibility b) {
        if (a instanceof RsVisibility.Private) return a;
        if (a instanceof RsVisibility.Public) return b;
        // a is Restricted
        if (b instanceof RsVisibility.Private) return b;
        if (b instanceof RsVisibility.Public) return a;
        // both Restricted
        RsMod aMod = ((RsVisibility.Restricted) a).getInMod();
        RsMod bMod = ((RsVisibility.Restricted) b).getInMod();
        return new RsVisibility.Restricted(
            aMod.getSuperMods().contains(bMod) ? aMod : bMod
        );
    }

    @Nonnull
    public static RsVisibility unite(@Nonnull RsVisibility a, @Nonnull RsVisibility b) {
        if (a instanceof RsVisibility.Restricted && b instanceof RsVisibility.Restricted) {
            RsMod aMod = ((RsVisibility.Restricted) a).getInMod();
            RsMod bMod = ((RsVisibility.Restricted) b).getInMod();
            RsMod commonParent = RsModUtil.commonParentMod(aMod, bMod);
            if (commonParent != null) {
                return new RsVisibility.Restricted(commonParent);
            }
            return RsVisibility.Public.INSTANCE;
        }
        if (a instanceof RsVisibility.Private && b instanceof RsVisibility.Private) {
            return RsVisibility.Private.INSTANCE;
        }
        return RsVisibility.Public.INSTANCE;
    }

    @Nonnull
    public static String format(@Nonnull RsVisibility visibility) {
        if (visibility instanceof RsVisibility.Private) return "";
        if (visibility instanceof RsVisibility.Public) return "pub ";
        RsMod inMod = ((RsVisibility.Restricted) visibility).getInMod();
        if (inMod.isCrateRoot()) {
            return "pub(crate) ";
        }
        return "pub(in crate" + inMod.getCrateRelativePath() + ") ";
    }

    @Nonnull
    public static RsVisStubKind getStubKind(@Nonnull RsVis vis) {
        org.rust.lang.core.stubs.RsVisStub stub = vis.getStub();
        if (stub != null) return stub.getKind();
        if (vis.getCrate() != null) return RsVisStubKind.CRATE;
        if (vis.getVisRestriction() != null) return RsVisStubKind.RESTRICTED;
        return RsVisStubKind.PUB;
    }

    @Nonnull
    public static RsVisibility getVisibility(@Nonnull RsVis vis) {
        RsVisStubKind kind = getStubKind(vis);
        switch (kind) {
            case PUB:
                return RsVisibility.Public.INSTANCE;
            case CRATE: {
                RsMod crateRoot = RsElementUtil.getCrateRoot(vis);
                return crateRoot != null
                    ? new RsVisibility.Restricted(crateRoot)
                    : RsVisibility.Public.INSTANCE;
            }
            case RESTRICTED: {
                RsVisRestriction restriction = vis.getVisRestriction();
                if (restriction == null) return RsVisibility.Public.INSTANCE;
                consulo.language.psi.PsiElement resolved = restriction.getPath().getReference() != null
                    ? restriction.getPath().getReference().resolve()
                    : null;
                if (resolved instanceof RsMod) {
                    return new RsVisibility.Restricted((RsMod) resolved);
                }
                return RsVisibility.Public.INSTANCE;
            }
            default:
                return RsVisibility.Public.INSTANCE;
        }
    }

    /**
     * Checks if a {@link RsVisible} element is public (has any non-private visibility).
     */
    public static boolean isPublic(@Nonnull RsVisibilityOwner owner) {
        return owner.isPublic();
    }

    @Nonnull
    public static RsVisibility getVisibility(@Nonnull RsVisibilityOwner owner) {
        return owner.getVisibility();
    }
}
