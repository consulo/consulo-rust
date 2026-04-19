/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import com.intellij.ide.util.MemberChooser;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class DialogStructMemberChooserUi implements StructMemberChooserUi {
    @NotNull
    private final String myTitle;
    private final boolean myAllowEmptySelection;

    DialogStructMemberChooserUi(@NotNull String title, boolean allowEmptySelection) {
        myTitle = title;
        myAllowEmptySelection = allowEmptySelection;
    }

    @Nullable
    @Override
    public List<RsStructMemberChooserObject> selectMembers(
        @NotNull Project project,
        @NotNull List<RsStructMemberChooserObject> all
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
