/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import consulo.ide.impl.idea.ide.util.MemberChooser;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

class DialogStructMemberChooserUi implements StructMemberChooserUi {
    @Nonnull
    private final String myTitle;
    private final boolean myAllowEmptySelection;

    DialogStructMemberChooserUi(@Nonnull String title, boolean allowEmptySelection) {
        myTitle = title;
        myAllowEmptySelection = allowEmptySelection;
    }

    @Nullable
    @Override
    public List<RsStructMemberChooserObject> selectMembers(
        @Nonnull Project project,
        @Nonnull List<RsStructMemberChooserObject> all
    ) {
        if (all.isEmpty()) {
            return all;
        }
        RsStructMemberChooserObject[] allArray = all.toArray(new RsStructMemberChooserObject[0]);
        MemberChooser<RsStructMemberChooserObject> chooser = new MemberChooser<>(allArray, myAllowEmptySelection, true, project);
        chooser.setTitle(myTitle);
        chooser.selectElements(allArray);
        chooser.setCopyJavadocVisible(false);
        chooser.show();
        return chooser.getSelectedElements();
    }
}
