/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.rust.lang.core.psi.ext.RsElementUtil;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;

public abstract class ResolvedPath {
    @Nonnull
    public abstract RsElement getElement();

    @Nonnull
    private Substitution mySubst = SubstitutionUtil.EMPTY_SUBSTITUTION;

    @Nonnull
    public Substitution getSubst() {
        return mySubst;
    }

    public void setSubst(@Nonnull Substitution subst) {
        mySubst = subst;
    }

    public static class Item extends ResolvedPath {
        @Nonnull
        private final RsElement myElement;
        private final boolean myIsVisible;

        public Item(@Nonnull RsElement element, boolean isVisible) {
            myElement = element;
            myIsVisible = isVisible;
        }

        @Nonnull
        @Override
        public RsElement getElement() {
            return myElement;
        }

        public boolean isVisible() {
            return myIsVisible;
        }
    }

    public static class AssocItem extends ResolvedPath {
        @Nonnull
        private final RsAbstractable myElement;
        @Nonnull
        private final TraitImplSource mySource;

        public AssocItem(@Nonnull RsAbstractable element, @Nonnull TraitImplSource source) {
            myElement = element;
            mySource = source;
        }

        @Nonnull
        @Override
        public RsAbstractable getElement() {
            return myElement;
        }

        @Nonnull
        public TraitImplSource getSource() {
            return mySource;
        }
    }

    @Nonnull
    public static ResolvedPath from(@Nonnull ScopeEntry entry, @Nonnull RsElement context) {
        if (entry instanceof AssocItemScopeEntry) {
            return new AssocItem(((AssocItemScopeEntry) entry).getElement(), ((AssocItemScopeEntry) entry).getSource());
        } else {
            org.rust.lang.core.psi.ext.RsMod contextMod = context.getContainingMod();
            org.rust.lang.core.psi.ext.RsElement element = entry.getElement();
            boolean isVisible =
                org.rust.lang.core.resolve.Processors.isVisibleFrom(entry, context)
                    && (!(element instanceof org.rust.lang.core.psi.ext.RsVisible)
                        || contextMod == null
                        || org.rust.lang.core.psi.ext.RsVisibilityUtil.isVisibleFrom(
                            (org.rust.lang.core.psi.ext.RsVisible) element, contextMod));
            return new Item(element, isVisible);
        }
    }

    @Nonnull
    public static ResolvedPath from(@Nonnull AssocItemScopeEntry entry) {
        return new AssocItem(entry.getElement(), entry.getSource());
    }
}
