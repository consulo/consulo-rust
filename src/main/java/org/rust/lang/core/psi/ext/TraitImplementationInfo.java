/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;

import java.util.*;
import java.util.stream.Collectors;

public final class TraitImplementationInfo {
    @NotNull public final RsTraitItem trait;
    @NotNull @NlsContexts.Label public final String traitName;
    @NotNull public final List<RsAbstractable> declared;
    @NotNull public final Map<String, RsAbstractable> declaredByNameAndType;
    @NotNull public final Map<String, RsAbstractable> implementedByNameAndType;
    @NotNull public final List<RsAbstractable> missingImplementations;
    @NotNull public final List<RsAbstractable> alreadyImplemented;

    private TraitImplementationInfo(
        @NotNull RsTraitItem trait,
        @NotNull String traitName,
        @NotNull RsMembers traitMembers,
        @NotNull RsMembers implMembers
    ) {
        this.trait = trait;
        this.traitName = traitName;

        List<RsAbstractable> allDeclared = abstractable(traitMembers);
        this.declared = allDeclared.stream()
            .filter(a -> !(a instanceof RsDocAndAttributeOwner) || RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf((RsDocAndAttributeOwner) a))
            .collect(Collectors.toList());

        List<RsAbstractable> implemented = abstractable(implMembers);

        this.declaredByNameAndType = new LinkedHashMap<>();
        for (RsAbstractable a : declared) {
            declaredByNameAndType.put(a.getName() + ":" + a.getNode().getElementType(), a);
        }

        this.implementedByNameAndType = new LinkedHashMap<>();
        for (RsAbstractable a : implemented) {
            implementedByNameAndType.put(a.getName() + ":" + a.getNode().getElementType(), a);
        }

        this.missingImplementations = declared.stream()
            .filter(RsAbstractable::isAbstract)
            .filter(it -> !implementedByNameAndType.containsKey(it.getName() + ":" + it.getNode().getElementType()))
            .collect(Collectors.toList());

        this.alreadyImplemented = declared.stream()
            .filter(RsAbstractable::isAbstract)
            .filter(it -> implementedByNameAndType.containsKey(it.getName() + ":" + it.getNode().getElementType()))
            .collect(Collectors.toList());
    }

    @NotNull
    private static List<RsAbstractable> abstractable(@NotNull RsMembers members) {
        return RsMembersUtil.getExpandedMembers(members).stream()
            .filter(it -> it.getName() != null)
            .collect(Collectors.toList());
    }

    @Nullable
    public static TraitImplementationInfo create(@NotNull RsTraitItem trait, @NotNull RsImplItem impl) {
        String traitName = trait.getName();
        if (traitName == null) return null;
        RsMembers traitMembers = trait.getMembers();
        if (traitMembers == null) return null;
        RsMembers implMembers = impl.getMembers();
        if (implMembers == null) return null;
        return new TraitImplementationInfo(trait, traitName, traitMembers, implMembers);
    }
}
