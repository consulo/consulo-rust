/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.implementMembers;

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.MemberChooserObject;
import com.intellij.codeInsight.generation.MemberChooserObjectBase;
import com.intellij.ui.SimpleColoredComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.presentation.PresentationInfo;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.RsAbstractable;

import javax.swing.*;

public class RsTraitMemberChooserMember implements ClassMember {
    @NotNull
    private final MemberChooserObjectBase myBase;
    @NotNull
    private final RsAbstractable myMember;
    @NotNull
    private final String myText;

    public RsTraitMemberChooserMember(@NotNull MemberChooserObjectBase base, @NotNull RsAbstractable member) {
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

    @NotNull
    public MemberChooserObjectBase getBase() {
        return myBase;
    }

    @NotNull
    public RsAbstractable getMember() {
        return myMember;
    }

    @Override
    public void renderTreeNode(@Nullable SimpleColoredComponent component, @Nullable JTree tree) {
        if (component != null) {
            component.setIcon(myMember.getIcon(0));
            component.append(myText);
        }
    }

    @Override
    public MemberChooserObject getParentNodeDelegate() {
        return myBase;
    }

    @NotNull
    @Override
    public String getText() {
        return myMember.getName() != null ? myMember.getName() : "";
    }

    @NotNull
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
