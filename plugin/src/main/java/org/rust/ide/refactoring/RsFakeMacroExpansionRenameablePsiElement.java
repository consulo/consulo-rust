/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.impl.psi.RenameableFakePsiElement;
import consulo.usage.UsageViewUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsReferenceElementBase;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;
import org.rust.lang.core.resolve.ref.RsReferenceBase;

import javax.swing.*;
import org.rust.lang.core.psi.ext.RsReferenceElementBase;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.ui.image.Image;

public abstract class RsFakeMacroExpansionRenameablePsiElement extends RenameableFakePsiElement implements PsiNameIdentifierOwner {
    @Nonnull
    private final RsNameIdentifierOwner myExpandedElement;

    protected RsFakeMacroExpansionRenameablePsiElement(
        @Nonnull RsNameIdentifierOwner expandedElement,
        @Nonnull PsiElement parent
    ) {
        super(parent);
        myExpandedElement = expandedElement;
    }

    @Nonnull
    public RsNameIdentifierOwner getExpandedElement() {
        return myExpandedElement;
    }

    @Nullable
    @Override
    public consulo.ui.image.Image getIcon() {
        return consulo.language.icon.IconDescriptorUpdaters.getIcon(myExpandedElement, 0);
    }

    @Nullable
    @Override
    public String getName() {
        return myExpandedElement.getName();
    }

    @Nonnull
    @Override
    public String getTypeName() {
        return UsageViewUtil.getType(myExpandedElement);
    }

    public static class AttrMacro extends RsFakeMacroExpansionRenameablePsiElement {
        @Nonnull
        private final RsNameIdentifierOwner mySourceElement;

        public AttrMacro(
            @Nonnull RsNameIdentifierOwner semantic,
            @Nonnull RsNameIdentifierOwner sourceElement
        ) {
            super(semantic, sourceElement.getParent());
            mySourceElement = sourceElement;
        }

        @Nullable
        @Override
        public PsiElement getNameIdentifier() {
            return mySourceElement.getNameIdentifier();
        }

        @Nonnull
        @Override
        public PsiElement setName(@Nonnull String name) {
            mySourceElement.setName(name);
            return this;
        }
    }

    public static class BangMacro extends RsFakeMacroExpansionRenameablePsiElement {
        @Nonnull
        private final RsReferenceElementBase mySourceElement;

        public BangMacro(
            @Nonnull RsNameIdentifierOwner semantic,
            @Nonnull RsReferenceElementBase sourceElement
        ) {
            super(semantic, sourceElement.getParent());
            mySourceElement = sourceElement;
        }

        @Nullable
        @Override
        public PsiElement getNameIdentifier() {
            return mySourceElement.getReferenceNameElement();
        }

        @Nonnull
        @Override
        public PsiElement setName(@Nonnull String name) {
            mySourceElement.getReference().handleElementRename(name);
            return this;
        }
    }

    public static class AttrPath extends RsFakeMacroExpansionRenameablePsiElement {
        @Nonnull
        private final PsiElement mySourceElement;

        public AttrPath(
            @Nonnull RsNameIdentifierOwner semantic,
            @Nonnull PsiElement sourceElement
        ) {
            super(semantic, sourceElement.getParent());
            mySourceElement = sourceElement;
        }

        @Nonnull
        @Override
        public PsiElement getNameIdentifier() {
            return mySourceElement;
        }

        @Nonnull
        @Override
        public PsiElement setName(@Nonnull String name) {
            RsReferenceBase.doRename(mySourceElement, name);
            return this;
        }
    }
}
