/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.utils.RsDiagnostic;
import com.intellij.psi.PsiElement;

import java.util.*;
import org.rust.lang.core.psi.ext.RsImplItemUtil;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;

public class RsTraitImplementationInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitImplItem2(@NotNull RsImplItem impl) {
                RsTraitRef traitRef = impl.getTraitRef();
                if (traitRef == null) return;
                RsTraitItem trait = RsTraitRefUtil.resolveToTrait(traitRef);
                if (trait == null) return;
                String traitName = trait.getName();
                if (traitName == null) return;

                TraitImplementationInfo implInfo = TraitImplementationInfo.create(trait, impl);
                if (implInfo == null) return;

                if (!RsImplItemUtil.isNegativeImpl(impl) && !implInfo.missingImplementations.isEmpty()) {
                    StringBuilder missing = new StringBuilder();
                    boolean first = true;
                    for (RsAbstractable m : implInfo.missingImplementations) {
                        String name = m.getName();
                        if (name != null) {
                            if (!first) missing.append(", ");
                            missing.append("`").append(name).append("`");
                            first = false;
                        }
                    }
                    PsiElement typeRef = impl.getTypeReference();
                    new RsDiagnostic.TraitItemsMissingImplError(
                        impl.getImpl(),
                        typeRef != null ? typeRef : impl.getImpl(),
                        missing.toString(),
                        impl
                    ).addToHolder(holder);
                }

                Set<String> traitMembersNames = new HashSet<>();
                for (RsAbstractable decl : implInfo.declared) {
                    String name = decl.getName();
                    if (name != null) traitMembersNames.add(name);
                }
                for (Map.Entry<String, RsAbstractable> entry : implInfo.implementedByNameAndType.entrySet()) {
                    String key = entry.getKey();
                    RsAbstractable implMember = entry.getValue();
                    // ignore members expanded from macros
                    if (implMember.getContainingFile() != impl.getContainingFile()) continue;

                    String memberName = key.contains(":") ? key.substring(0, key.indexOf(':')) : key;
                    if (traitMembersNames.contains(memberName)) {
                        RsAbstractable traitMember = implInfo.declaredByNameAndType.get(key);

                        if (implMember instanceof RsFunction && traitMember instanceof RsFunction) {
                            checkTraitFnImplParams(holder, (RsFunction) implMember, (RsFunction) traitMember, traitName);
                        }

                        if (traitMember == null) {
                            PsiElement nameIdentifier = implMember.getNameIdentifier();
                            if (nameIdentifier == null) continue;
                            new RsDiagnostic.MismatchMemberInTraitImplError(nameIdentifier, implMember, traitName)
                                .addToHolder(holder);
                        }
                    } else {
                        PsiElement nameIdentifier = implMember.getNameIdentifier();
                        if (nameIdentifier != null) {
                            new RsDiagnostic.UnknownMemberInTraitError(nameIdentifier, implMember, traitName)
                                .addToHolder(holder);
                        }
                    }
                }
            }
        };
    }

    private static void checkTraitFnImplParams(
        @NotNull RsProblemsHolder holder,
        @NotNull RsFunction fn,
        @NotNull RsFunction superFn,
        @NotNull String traitName
    ) {
        RsValueParameterList params = fn.getValueParameterList();
        if (params == null) return;
        RsSelfParameter selfArg = fn.getSelfParameter();
        RsSelfParameter superSelfParameter = superFn.getSelfParameter();

        if (selfArg != null && superFn.getSelfParameter() == null) {
            new RsDiagnostic.DeclMissingFromTraitError(selfArg, fn, superFn, selfArg).addToHolder(holder);
        } else if (selfArg == null && superSelfParameter != null) {
            new RsDiagnostic.DeclMissingFromImplError(params, fn, superFn, superSelfParameter).addToHolder(holder);
        }

        int paramsCount = fn.getValueParameters().size();
        int superParamsCount = superFn.getValueParameters().size();
        if (paramsCount != superParamsCount) {
            new RsDiagnostic.TraitParamCountMismatchError(params, fn, traitName, paramsCount, superParamsCount)
                .addToHolder(holder);
        }
    }
}
