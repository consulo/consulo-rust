/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.rust.lang.core.psi.ext.RsElementUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;

public abstract class ResolvedPath {
    @NotNull
    public abstract RsElement getElement();

    @NotNull
    private Substitution mySubst = SubstitutionUtil.EMPTY_SUBSTITUTION;

    @NotNull
    public Substitution getSubst() {
        return mySubst;
    }

    public void setSubst(@NotNull Substitution subst) {
        mySubst = subst;
    }

    public static class Item extends ResolvedPath {
        @NotNull
        private final RsElement myElement;
        private final boolean myIsVisible;

        public Item(@NotNull RsElement element, boolean isVisible) {
            myElement = element;
            myIsVisible = isVisible;
        }

        @NotNull
        @Override
        public RsElement getElement() {
            return myElement;
        }

        public boolean isVisible() {
            return myIsVisible;
        }
    }

    public static class AssocItem extends ResolvedPath {
        @NotNull
        private final RsAbstractable myElement;
        @NotNull
        private final TraitImplSource mySource;

        public AssocItem(@NotNull RsAbstractable element, @NotNull TraitImplSource source) {
            myElement = element;
            mySource = source;
        }

        @NotNull
        @Override
        public RsAbstractable getElement() {
            return myElement;
        }

        @NotNull
        public TraitImplSource getSource() {
            return mySource;
        }
    }

    @NotNull
    public static ResolvedPath from(@NotNull ScopeEntry entry, @NotNull RsElement context) {
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

    @NotNull
    public static ResolvedPath from(@NotNull AssocItemScopeEntry entry) {
        return new AssocItem(entry.getElement(), entry.getSource());
    }
}
