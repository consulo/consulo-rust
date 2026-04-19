/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.generate;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.openapiext.OpenApiUtil;

import java.util.List;
import java.util.stream.Collectors;

public interface StructMemberChooserUi {
    @Nullable
    List<RsStructMemberChooserObject> selectMembers(
        @NotNull Project project,
        @NotNull List<RsStructMemberChooserObject> all
    );

    @Nullable
    static List<StructMember> showStructMemberChooserDialog(
        @NotNull Project project,
        @NotNull RsStructItem structItem,
        @NotNull List<StructMember> fields,
        @NotNull String title,
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

        com.intellij.codeInsight.generation.MemberChooserObjectBase base =
            new com.intellij.codeInsight.generation.MemberChooserObjectBase(structItem.getName(), structItem.getIcon(0));
        List<RsStructMemberChooserObject> arguments = fields.stream()
            .map(f -> new RsStructMemberChooserObject(base, f))
            .collect(Collectors.toList());
        List<RsStructMemberChooserObject> chosen = chooser.selectMembers(project, arguments);
        if (chosen == null) return null;
        return chosen.stream().map(RsStructMemberChooserObject::getMember).collect(Collectors.toList());
    }

    @TestOnly
    static void withMockStructMemberChooserUi(@NotNull StructMemberChooserUi mockUi, @NotNull Runnable action) {
        StructMemberChooserUiHolder.MOCK = mockUi;
        try {
            action.run();
        } finally {
            StructMemberChooserUiHolder.MOCK = null;
        }
    }
}
