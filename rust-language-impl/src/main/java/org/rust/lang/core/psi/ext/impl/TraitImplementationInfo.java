/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;


import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.*;

public final class TraitImplementationInfo {
    @Nonnull public final RsTraitItem trait;
    @Nonnull  public final String traitName;
    @Nonnull public final List<RsAbstractable> declared;
    @Nonnull public final Map<String, RsAbstractable> declaredByNameAndType;
    @Nonnull public final Map<String, RsAbstractable> implementedByNameAndType;
    @Nonnull public final List<RsAbstractable> missingImplementations;
    @Nonnull public final List<RsAbstractable> alreadyImplemented;

    private TraitImplementationInfo(
        @Nonnull RsTraitItem trait,
        @Nonnull String traitName,
        @Nonnull RsMembers traitMembers,
        @Nonnull RsMembers implMembers
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

    @Nonnull
    private static List<RsAbstractable> abstractable(@Nonnull RsMembers members) {
        return RsMembersUtil.getExpandedMembers(members).stream()
            .filter(it -> it.getName() != null)
            .collect(Collectors.toList());
    }

    @Nullable
    public static TraitImplementationInfo create(@Nonnull RsTraitItem trait, @Nonnull RsImplItem impl) {
        String traitName = trait.getName();
        if (traitName == null) return null;
        RsMembers traitMembers = trait.getMembers();
        if (traitMembers == null) return null;
        RsMembers implMembers = impl.getMembers();
        if (implMembers == null) return null;
        return new TraitImplementationInfo(trait, traitName, traitMembers, implMembers);
    }
}
