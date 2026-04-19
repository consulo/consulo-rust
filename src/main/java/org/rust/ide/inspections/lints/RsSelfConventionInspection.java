/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.infer.InferExtUtil;
import org.rust.lang.core.types.SelfTypeExtUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyReference;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;

public class RsSelfConventionInspection extends RsLintInspection {

    private static final List<SelfConvention> SELF_CONVENTIONS = Arrays.asList(
        new SelfConvention("as_", Arrays.asList(SelfSignature.BY_REF, SelfSignature.BY_MUT_REF)),
        new SelfConvention("from_", Collections.singletonList(SelfSignature.NO_SELF)),
        new SelfConvention("into_", Collections.singletonList(SelfSignature.BY_VAL)),
        new SelfConvention("is_", Arrays.asList(SelfSignature.BY_REF, SelfSignature.NO_SELF)),
        new SelfConvention("to_", Collections.singletonList(SelfSignature.BY_MUT_REF), "_mut"),
        new SelfConvention("to_", Arrays.asList(SelfSignature.BY_REF, SelfSignature.BY_VAL))
    );

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.WrongSelfConvention;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitFunction2(@NotNull RsFunction m) {
                RsAbstractableOwner owner = m.getOwner();
                PsiElement traitOrImpl;
                if (owner instanceof RsAbstractableOwner.Trait) {
                    traitOrImpl = ((RsAbstractableOwner.Trait) owner).getTrait();
                } else if (owner instanceof RsAbstractableOwner.Impl) {
                    RsAbstractableOwner.Impl implOwner = (RsAbstractableOwner.Impl) owner;
                    traitOrImpl = implOwner.isInherent() ? implOwner.getImpl() : null;
                } else {
                    traitOrImpl = null;
                }
                if (traitOrImpl == null) return;

                String identifierText = m.getIdentifier().getText();
                SelfConvention convention = null;
                for (SelfConvention c : SELF_CONVENTIONS) {
                    String postfix = c.getPostfix() != null ? c.getPostfix() : "";
                    if (identifierText.startsWith(c.getPrefix()) && identifierText.endsWith(postfix)) {
                        convention = c;
                        break;
                    }
                }
                if (convention == null) return;

                SelfSignature selfSignature = getSelfSignature(m);
                if (convention.getSelfSignatures().contains(selfSignature)) return;

                // Ignore the inspection if the self type is arbitrary
                if (selfSignature instanceof SelfSignature.ArbitrarySelfSignature
                    && !convention.getSelfSignatures().equals(Collections.singletonList(SelfSignature.NO_SELF))) {
                    return;
                }

                if (selfSignature == SelfSignature.BY_VAL) {
                    Ty selfType = org.rust.lang.core.types.ExtensionsUtil.getSelfType((RsTraitOrImpl) traitOrImpl);
                    ImplLookup implLookup = ImplLookup.relativeTo((RsElement) traitOrImpl);
                    if (selfType instanceof TyUnknown || !implLookup.isCopy(selfType).isFalse()) return;
                }

                PsiElement problemElement = m.getSelfParameter() != null ? m.getSelfParameter() : m.getIdentifier();
                registerConventionProblem(holder, problemElement, convention);
            }
        };
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }

    @NotNull
    private static SelfSignature getSelfSignature(@NotNull RsFunction function) {
        RsSelfParameter self = function.getSelfParameter();
        if (self == null) return SelfSignature.NO_SELF;

        if (RsSelfParameterUtil.isExplicitType(self)) {
            Ty expectedSelfType = InferExtUtil.selfType(function);
            if (expectedSelfType == null) expectedSelfType = TyUnknown.INSTANCE;
            Ty actualSelfType = InferExtUtil.typeOfValue(self);

            if (expectedSelfType.isEquivalentTo(actualSelfType)) {
                return SelfSignature.BY_VAL;
            }
            if (actualSelfType instanceof TyReference) {
                TyReference ref = (TyReference) actualSelfType;
                boolean mutable = ref.getMutability().isMut();
                if (expectedSelfType.isEquivalentTo(ref.getReferenced())) {
                    return mutable ? SelfSignature.BY_MUT_REF : SelfSignature.BY_REF;
                }
            }
            return SelfSignature.ArbitrarySelfSignature.INSTANCE;
        } else {
            if (RsSelfParameterUtil.isRef(self) && RsSelfParameterUtil.getMutability(self).isMut()) {
                return SelfSignature.BY_MUT_REF;
            }
            if (RsSelfParameterUtil.isRef(self)) {
                return SelfSignature.BY_REF;
            }
            return SelfSignature.BY_VAL;
        }
    }

    private static void registerConventionProblem(
        @NotNull RsProblemsHolder holder,
        @NotNull PsiElement element,
        @NotNull SelfConvention convention
    ) {
        String selfTypes = convention.getSelfSignatures().stream()
            .map(SelfSignature.BasicSelfSignature::getDescription)
            .collect(Collectors.joining(" or "));

        String postfix = convention.getPostfix() != null ? convention.getPostfix() : "";
        String description = RsBundle.message(
            "inspection.message.methods.called.usually.take.consider.choosing.less.ambiguous.name",
            convention.getPrefix(), postfix, selfTypes
        );
        holder.registerProblem(element, description);
    }
}
