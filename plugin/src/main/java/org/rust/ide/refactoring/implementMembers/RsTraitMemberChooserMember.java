/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.implementMembers;

import consulo.language.editor.generation.ClassMember;
import consulo.language.editor.generation.MemberChooserObject;
import consulo.language.editor.generation.MemberChooserObjectBase;
import consulo.ui.ex.awt.SimpleColoredComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.presentation.PresentationInfo;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.RsAbstractable;

import javax.swing.*;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.ui.ex.ColoredTextContainer;

public class RsTraitMemberChooserMember implements ClassMember {
    @Nonnull
    private final MemberChooserObjectBase myBase;
    @Nonnull
    private final RsAbstractable myMember;
    @Nonnull
    private final String myText;

    public RsTraitMemberChooserMember(@Nonnull MemberChooserObjectBase base, @Nonnull RsAbstractable member) {
        myBase = base;
        myMember = member;
        if (member instanceof RsFunction || member instanceof RsTypeAlias) {
            org.rust.ide.presentation.PresentationInfo info = PresentationInfo.getPresentationInfo(member);
            myText = info != null ? info.getProjectStructureItemText() : "";
        } else if (member instanceof RsConstant) {
            RsConstant constant = (RsConstant) member;
            String typeText = constant.getTypeReference() != null ? constant.getTypeReference().getText() : "";
            myText = constant.getName() + ": " + typeText;
        } else {
            throw new IllegalStateException("Unknown trait member: " + member);
        }
    }

    @Nonnull
    public MemberChooserObjectBase getBase() {
        return myBase;
    }

    @Nonnull
    public RsAbstractable getMember() {
        return myMember;
    }

    @Override
    public void renderTreeNode(consulo.ui.ex.ColoredTextContainer component, JTree tree) {
        if (component != null) {
            component.setIcon(consulo.language.icon.IconDescriptorUpdaters.getIcon(myMember, 0));
            component.append(myText);
        }
    }

    @Override
    public MemberChooserObject getParentNodeDelegate() {
        return myBase;
    }

    @Nonnull
    @Override
    public String getText() {
        return myMember.getName() != null ? myMember.getName() : "";
        }

    @Nonnull
    public String formattedText() {
        return myText;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RsTraitMemberChooserMember) {
            return myText.equals(((RsTraitMemberChooserMember) other).myText);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return myText.hashCode();
    }
}
