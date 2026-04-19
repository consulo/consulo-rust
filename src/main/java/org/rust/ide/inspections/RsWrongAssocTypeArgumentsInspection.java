/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.completion.CompletionUtilsUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.utils.RsDiagnostic;

import java.util.*;
// CompletionUtilsUtil removed
import org.rust.lang.core.psi.ext.RsTraitItemUtil;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Inspection that detects the E0191 and E0220 errors.
 */
public class RsWrongAssocTypeArgumentsInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitTraitRef(@NotNull RsTraitRef trait) {
                checkAssocTypes(holder, trait, trait.getPath());
            }

            @Override
            public void visitPathType(@NotNull RsPathType type) {
                if ("Self".equals(type.getPath().getReferenceName())) return;
                checkAssocTypes(holder, type, type.getPath());
            }
        };
    }

    private void checkAssocTypes(@NotNull RsProblemsHolder holder, @NotNull RsElement element, @NotNull RsPath path) {
        PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
        if (!(resolved instanceof RsTraitItem)) return;
        RsTraitItem trait = (RsTraitItem) resolved;
        RsTypeArgumentList arguments = path.getTypeArgumentList();
        List<RsAssocTypeBinding> assocArguments = arguments != null ? arguments.getAssocTypeBindingList() : null;
        Map<String, RsAssocTypeBinding> assocArgumentMap = new LinkedHashMap<>();
        if (assocArguments != null) {
            for (RsAssocTypeBinding binding : assocArguments) {
                RsPath bindingPath = binding.getPath();
                if (bindingPath.getHasColonColon()) continue;
                String name = bindingPath.getReferenceName();
                if (name == null) continue;
                assocArgumentMap.put(name, binding);
            }
        }

        Map<String, RsTypeAlias> assocTypes = new LinkedHashMap<>();
        for (RsTypeAlias typeAlias : RsTraitItemUtil.getAssociatedTypesTransitively(trait)) {
            assocTypes.put(typeAlias.getIdentifier().getText(), typeAlias);
        }

        if (assocArguments != null) {
            checkUnknownAssocTypes(holder, assocArgumentMap, assocTypes, trait);
        }

        PsiElement parent = element.getParent();
        // Do not check missing associated types in:
        // super trait and generic bounds
        // impl Trait for ...
        // Fn traits
        // impl Trait
        // type qual
        if (RsElementUtil.ancestorStrict(element, RsTypeParamBounds.class) != null
            || (parent instanceof RsImplItem && ((RsImplItem) parent).getTraitRef() == element)
            || CompletionUtilsUtil.isFnLikeTrait(trait)
            || isImplTrait(element)
            || element.getParent() instanceof RsTypeQual) {
            return;
        }

        checkMissingAssocTypes(holder, element, assocArgumentMap, assocTypes);
    }

    private void checkUnknownAssocTypes(
        @NotNull RsProblemsHolder holder,
        @NotNull Map<String, RsAssocTypeBinding> assocArguments,
        @NotNull Map<String, RsTypeAlias> assocTypes,
        @NotNull RsTraitItem trait
    ) {
        for (Map.Entry<String, RsAssocTypeBinding> entry : assocArguments.entrySet()) {
            String name = entry.getKey();
            RsAssocTypeBinding argument = entry.getValue();
            if (!assocTypes.containsKey(name)) {
                String traitName = trait.getName();
                if (traitName == null) continue;
                new RsDiagnostic.UnknownAssocTypeBinding(argument, name, traitName).addToHolder(holder);
            }
        }
    }

    private void checkMissingAssocTypes(
        @NotNull RsProblemsHolder holder,
        @NotNull RsElement element,
        @NotNull Map<String, RsAssocTypeBinding> assocArgumentMap,
        @NotNull Map<String, RsTypeAlias> requiredAssocTypes
    ) {
        List<MissingAssocTypeBinding> missingTypes = new ArrayList<>();
        for (Map.Entry<String, RsTypeAlias> entry : requiredAssocTypes.entrySet()) {
            String name = entry.getKey();
            RsTypeAlias assocType = entry.getValue();
            if (!assocArgumentMap.containsKey(name)) {
                RsAbstractableOwner owner = RsAbstractableUtil.getOwner(assocType);
                if (!(owner instanceof RsAbstractableOwner.Trait)) continue;
                RsTraitItem ownerTrait = ((RsAbstractableOwner.Trait) owner).getTrait();
                String traitName = ownerTrait.getName();
                if (traitName == null) continue;
                missingTypes.add(new MissingAssocTypeBinding(name, traitName));
            }
        }
        if (!missingTypes.isEmpty()) {
            missingTypes.sort(Comparator.comparing(m -> m.myName));
            new RsDiagnostic.MissingAssocTypeBindings(element, missingTypes).addToHolder(holder);
        }
    }

    public static class MissingAssocTypeBinding {
        public final String myName;
        public final String myTrait;

        public MissingAssocTypeBinding(@NotNull String name, @NotNull String trait) {
            this.myName = name;
            this.myTrait = trait;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MissingAssocTypeBinding)) return false;
            MissingAssocTypeBinding that = (MissingAssocTypeBinding) o;
            return myName.equals(that.myName) && myTrait.equals(that.myTrait);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myName, myTrait);
        }

        @Override
        public String toString() {
            return "MissingAssocTypeBinding(name=" + myName + ", trait=" + myTrait + ")";
        }
    }

    private static boolean isImplTrait(@NotNull PsiElement element) {
        if (!(element instanceof RsTraitRef)) return false;
        PsiElement grandparent = element.getParent() != null ? element.getParent().getParent() : null;
        if (!(grandparent instanceof RsPolybound)) return false;
        PsiElement traitType = grandparent.getParent();
        if (!(traitType instanceof RsTraitType)) return false;
        return ((RsTraitType) traitType).getImpl() != null;
    }
}
