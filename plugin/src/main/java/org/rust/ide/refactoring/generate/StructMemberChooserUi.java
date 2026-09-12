/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.lang.core.psi.RsStructItem;
import org.rust.openapiext.OpenApiUtil;

import java.util.List;
import java.util.stream.Collectors;
import consulo.language.editor.generation.MemberChooserObjectBase;
import consulo.language.icon.IconDescriptorUpdaters;

public interface StructMemberChooserUi {
    @Nullable
    List<RsStructMemberChooserObject> selectMembers(
        @Nonnull Project project,
        @Nonnull List<RsStructMemberChooserObject> all
    );

    @Nullable
    static List<StructMember> showStructMemberChooserDialog(
        @Nonnull Project project,
        @Nonnull RsStructItem structItem,
        @Nonnull List<StructMember> fields,
        @Nonnull String title,
        boolean allowEmptySelection
    ) {
        StructMemberChooserUi chooser;
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            chooser = StructMemberChooserUiHolder.MOCK;
            if (chooser == null) {
                throw new IllegalStateException("You should set mock ui via `withMockStructMemberChooserUi`");
            }
        } else {
            chooser = new DialogStructMemberChooserUi(title, allowEmptySelection);
        }

        consulo.language.editor.generation.MemberChooserObjectBase base =
            new consulo.language.editor.generation.MemberChooserObjectBase(structItem.getName(), consulo.language.icon.IconDescriptorUpdaters.getIcon(structItem, 0));
        List<RsStructMemberChooserObject> arguments = fields.stream()
            .map(f -> new RsStructMemberChooserObject(base, f))
            .collect(Collectors.toList());
        List<RsStructMemberChooserObject> chosen = chooser.selectMembers(project, arguments);
        if (chosen == null) return null;
        return chosen.stream().map(RsStructMemberChooserObject::getMember).collect(Collectors.toList());
    }

    
    static void withMockStructMemberChooserUi(@Nonnull StructMemberChooserUi mockUi, @Nonnull Runnable action) {
        StructMemberChooserUiHolder.MOCK = mockUi;
        try {
            action.run();
        } finally {
            StructMemberChooserUiHolder.MOCK = null;
        }
    }
}
