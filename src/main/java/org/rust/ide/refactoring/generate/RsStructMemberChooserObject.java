/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import com.intellij.codeInsight.generation.ClassMember;
import com.intellij.codeInsight.generation.MemberChooserObject;
import com.intellij.codeInsight.generation.MemberChooserObjectBase;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.icons.RsIcons;

public class RsStructMemberChooserObject extends MemberChooserObjectBase implements ClassMember {
    @NotNull
    private final MemberChooserObjectBase myBase;
    @NotNull
    private final StructMember myMember;

    public RsStructMemberChooserObject(@NotNull MemberChooserObjectBase base, @NotNull StructMember member) {
        super(member.getDialogRepresentation(), RsIcons.FIELD);
        myBase = base;
        myMember = member;
    }

    @NotNull
    public MemberChooserObjectBase getBase() {
        return myBase;
    }

    @NotNull
    public StructMember getMember() {
        return myMember;
    }

    @Override
    public MemberChooserObject getParentNodeDelegate() {
        return myBase;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RsStructMemberChooserObject) {
            return myMember.equals(((RsStructMemberChooserObject) other).myMember);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getText().hashCode();
    }
}
