/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.impl.RenameableFakePsiElement;
import com.intellij.usageView.UsageViewUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsReferenceElementBase;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;
import org.rust.lang.core.resolve.ref.RsReferenceBase;

import javax.swing.*;
import org.rust.lang.core.psi.ext.RsReferenceElementBase;

public abstract class RsFakeMacroExpansionRenameablePsiElement extends RenameableFakePsiElement implements PsiNameIdentifierOwner {
    @NotNull
    private final RsNameIdentifierOwner myExpandedElement;

    protected RsFakeMacroExpansionRenameablePsiElement(
        @NotNull RsNameIdentifierOwner expandedElement,
        @NotNull PsiElement parent
    ) {
        super(parent);
        myExpandedElement = expandedElement;
    }

    @NotNull
    public RsNameIdentifierOwner getExpandedElement() {
        return myExpandedElement;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return myExpandedElement.getIcon(0);
    }

    @Nullable
    @Override
    public String getName() {
        return myExpandedElement.getName();
    }

    @NotNull
    @Override
    public String getTypeName() {
        return UsageViewUtil.getType(myExpandedElement);
    }

    public static class AttrMacro extends RsFakeMacroExpansionRenameablePsiElement {
        @NotNull
        private final RsNameIdentifierOwner mySourceElement;

        public AttrMacro(
            @NotNull RsNameIdentifierOwner semantic,
            @NotNull RsNameIdentifierOwner sourceElement
        ) {
            super(semantic, sourceElement.getParent());
            mySourceElement = sourceElement;
        }

        @Nullable
        @Override
        public PsiElement getNameIdentifier() {
            return mySourceElement.getNameIdentifier();
        }

        @NotNull
        @Override
        public PsiElement setName(@NotNull String name) {
            mySourceElement.setName(name);
            return this;
        }
    }

    public static class BangMacro extends RsFakeMacroExpansionRenameablePsiElement {
        @NotNull
        private final RsReferenceElementBase mySourceElement;

        public BangMacro(
            @NotNull RsNameIdentifierOwner semantic,
            @NotNull RsReferenceElementBase sourceElement
        ) {
            super(semantic, sourceElement.getParent());
            mySourceElement = sourceElement;
        }

        @Nullable
        @Override
        public PsiElement getNameIdentifier() {
            return mySourceElement.getReferenceNameElement();
        }

        @NotNull
        @Override
        public PsiElement setName(@NotNull String name) {
            mySourceElement.getReference().handleElementRename(name);
            return this;
        }
    }

    public static class AttrPath extends RsFakeMacroExpansionRenameablePsiElement {
        @NotNull
        private final PsiElement mySourceElement;

        public AttrPath(
            @NotNull RsNameIdentifierOwner semantic,
            @NotNull PsiElement sourceElement
        ) {
            super(semantic, sourceElement.getParent());
            mySourceElement = sourceElement;
        }

        @NotNull
        @Override
        public PsiElement getNameIdentifier() {
            return mySourceElement;
        }

        @NotNull
        @Override
        public PsiElement setName(@NotNull String name) {
            RsReferenceBase.doRename(mySourceElement, name);
            return this;
        }
    }
}
