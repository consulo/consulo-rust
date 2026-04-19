/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.implementMembers;

import com.intellij.codeInsight.generation.MemberChooserObjectBase;
import com.intellij.ide.util.MemberChooser;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.RsBundle;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.TraitImplementationInfo;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import java.util.stream.Collectors;

public final class TraitMemberChooserUi {

    @Nullable
    private static TraitMemberChooser MOCK = null;

    private TraitMemberChooserUi() {
    }

    @NotNull
    public static Collection<RsAbstractable> showTraitMemberChooser(
        @NotNull TraitImplementationInfo implInfo,
        @NotNull Project project
    ) {
        MemberChooserObjectBase base = new MemberChooserObjectBase(implInfo.traitName, implInfo.trait.getIcon(0));
        List<RsTraitMemberChooserMember> all = implInfo.declared.stream()
            .map(it -> new RsTraitMemberChooserMember(base, it))
            .collect(Collectors.toList());
        Set<RsAbstractable> alreadyImplemented = new HashSet<>(implInfo.alreadyImplemented);
        List<RsTraitMemberChooserMember> nonImplemented = all.stream()
            .filter(it -> !alreadyImplemented.contains(it.getMember()))
            .collect(Collectors.toList());
        Set<RsAbstractable> missingImplementations = new HashSet<>(implInfo.missingImplementations);
        List<RsTraitMemberChooserMember> selectedByDefault = nonImplemented.stream()
            .filter(it -> missingImplementations.contains(it.getMember()))
            .collect(Collectors.toList());

        TraitMemberChooser chooser = org.rust.openapiext.OpenApiUtil.isUnitTestMode() ? MOCK : TraitMemberChooserUi::memberChooserDialog;
        if (chooser == null) {
            throw new IllegalStateException("MOCK not set in unit test mode");
        }
        List<RsTraitMemberChooserMember> chosen = chooser.choose(project, nonImplemented, selectedByDefault);
        return chosen.stream().map(RsTraitMemberChooserMember::getMember).collect(Collectors.toList());
    }

    @NotNull
    private static List<RsTraitMemberChooserMember> memberChooserDialog(
        @NotNull Project project,
        @NotNull List<RsTraitMemberChooserMember> all,
        @NotNull List<RsTraitMemberChooserMember> selectedByDefault
    ) {
        RsTraitMemberChooserMember[] allArray = all.toArray(new RsTraitMemberChooserMember[0]);
        MemberChooser<RsTraitMemberChooserMember> chooser = new MemberChooser<>(allArray, true, true, project);
        chooser.setTitle(RsBundle.message("dialog.title.implement.members"));
        chooser.selectElements(selectedByDefault.toArray(new RsTraitMemberChooserMember[0]));
        chooser.setCopyJavadocVisible(false);
        chooser.show();
        List<RsTraitMemberChooserMember> selected = chooser.getSelectedElements();
        return selected != null ? selected : Collections.emptyList();
    }

    @TestOnly
    public static void withMockTraitMemberChooser(@NotNull TraitMemberChooser mock, @NotNull Runnable action) {
        MOCK = (project, all, selectedByDefault) -> {
            List<RsTraitMemberChooserMember> result = mock.choose(project, all, selectedByDefault);
            MOCK = null;
            return result;
        };
        try {
            action.run();
            if (MOCK != null) {
                throw new IllegalStateException("Selector was not called");
            }
        } finally {
            MOCK = null;
        }
    }

    @FunctionalInterface
    public interface TraitMemberChooser {
        @NotNull
        List<RsTraitMemberChooserMember> choose(
            @NotNull Project project,
            @NotNull List<RsTraitMemberChooserMember> all,
            @NotNull List<RsTraitMemberChooserMember> selectedByDefault
        );
    }
}
