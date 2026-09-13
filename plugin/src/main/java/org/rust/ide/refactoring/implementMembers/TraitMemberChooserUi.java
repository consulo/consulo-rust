/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.implementMembers;

import consulo.language.editor.generation.MemberChooserObjectBase;
import consulo.ide.impl.idea.ide.util.MemberChooser;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.RsBundle;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.impl.TraitImplementationInfo;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import java.util.stream.Collectors;
import consulo.language.icon.IconDescriptorUpdaters;

public final class TraitMemberChooserUi {

    @Nullable
    private static TraitMemberChooser MOCK = null;

    private TraitMemberChooserUi() {
    }

    @Nonnull
    public static Collection<RsAbstractable> showTraitMemberChooser(
        @Nonnull TraitImplementationInfo implInfo,
        @Nonnull Project project
    ) {
        MemberChooserObjectBase base = new MemberChooserObjectBase(implInfo.traitName, consulo.language.icon.IconDescriptorUpdaters.getIcon(implInfo.trait, 0));
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

    @Nonnull
    private static List<RsTraitMemberChooserMember> memberChooserDialog(
        @Nonnull Project project,
        @Nonnull List<RsTraitMemberChooserMember> all,
        @Nonnull List<RsTraitMemberChooserMember> selectedByDefault
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

    
    public static void withMockTraitMemberChooser(@Nonnull TraitMemberChooser mock, @Nonnull Runnable action) {
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
        @Nonnull
        List<RsTraitMemberChooserMember> choose(
            @Nonnull Project project,
            @Nonnull List<RsTraitMemberChooserMember> all,
            @Nonnull List<RsTraitMemberChooserMember> selectedByDefault
        );
    }
}
