/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import consulo.language.editor.generation.ClassMember;
import consulo.language.editor.generation.MemberChooserObject;
import consulo.language.editor.generation.MemberChooserObjectBase;
import jakarta.annotation.Nonnull;
import org.rust.icons.RsIcons;

public class RsStructMemberChooserObject extends MemberChooserObjectBase implements ClassMember {
    @Nonnull
    private final MemberChooserObjectBase myBase;
    @Nonnull
    private final StructMember myMember;

    public RsStructMemberChooserObject(@Nonnull MemberChooserObjectBase base, @Nonnull StructMember member) {
        super(member.getDialogRepresentation(), RsIcons.FIELD);
        myBase = base;
        myMember = member;
    }

    @Nonnull
    public MemberChooserObjectBase getBase() {
        return myBase;
    }

    @Nonnull
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
