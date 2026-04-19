/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.suggested;

import com.intellij.psi.PsiNamedElement;
import com.intellij.refactoring.suggested.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.refactoring.changeSignature.RsChangeSignatureHandler;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;

public class RsSuggestedRefactoringAvailability extends SuggestedRefactoringAvailability {

    public RsSuggestedRefactoringAvailability(@NotNull RsSuggestedRefactoringSupport support) {
        super(support);
    }

    @Nullable
    @Override
    public SuggestedRefactoringData detectAvailableRefactoring(@NotNull SuggestedRefactoringState state) {
        if (state.getDeclaration() instanceof RsFunction) {
            RsFunction function = (RsFunction) state.getDeclaration();
            if (hasComplexChanges(function, state.getOldSignature(), state.getNewSignature())) {
                String name = function.getName() != null ? function.getName() : "";
                return SuggestedChangeSignatureData.create(state, name);
            }
        }
        if (!(state.getDeclaration() instanceof PsiNamedElement)) return null;
        PsiNamedElement namedElement = (PsiNamedElement) state.getDeclaration();
        return new SuggestedRenameData(namedElement, state.getOldSignature().getName());
    }

    @Override
    public boolean shouldSuppressRefactoringForDeclaration(@NotNull SuggestedRefactoringState state) {
        if (state.getDeclaration() instanceof RsFunction) {
            RsFunction function = (RsFunction) state.restoredDeclarationCopy();
            if (function == null) return false;
            return !RsChangeSignatureHandler.isChangeSignatureAvailable(function);
        }
        if (state.getDeclaration() instanceof RsPatBinding) {
            RsPatBinding binding = (RsPatBinding) state.restoredDeclarationCopy();
            if (binding == null) return false;
            return RsPatBindingUtil.isReferenceToConstant(binding);
        }
        return false;
    }

    private boolean hasComplexChanges(
        @NotNull RsFunction function,
        @NotNull SuggestedRefactoringSupport.Signature oldSignature,
        @NotNull SuggestedRefactoringSupport.Signature newSignature
    ) {
        // Condition order is important here.
        // hasTypeChanges cannot be called if parameters were removed or added
        if (hasParameterAddedRemovedOrReordered(oldSignature, newSignature)) return true;

        // Type changes can only be observed by child method signatures
        // function.owner is not used here on purpose, to avoid using resolve
        if (function.getParent() instanceof RsMembers
            && function.getParent().getParent() instanceof RsTraitItem
            && hasTypeChanges(oldSignature, newSignature)) return true;

        return hasNameChanges(oldSignature, newSignature);
    }

    /**
     * Find if any parameter with a simple identifier was renamed.
     */
    private static boolean hasNameChanges(
        @NotNull SuggestedRefactoringSupport.Signature oldSignature,
        @NotNull SuggestedRefactoringSupport.Signature newSignature
    ) {
        for (SuggestedRefactoringSupport.Parameter parameter : newSignature.getParameters()) {
            SuggestedRefactoringSupport.Parameter oldParam = oldSignature.parameterById(parameter.getId());
            if (oldParam == null) continue;
            SuggestedRefactoringSupport.ParameterAdditionalData oldDataRaw = oldParam.getAdditionalData();
            SuggestedRefactoringSupport.ParameterAdditionalData newDataRaw = parameter.getAdditionalData();
            if (!(oldDataRaw instanceof RsParameterAdditionalData)) continue;
            if (!(newDataRaw instanceof RsParameterAdditionalData)) continue;
            RsParameterAdditionalData oldData = (RsParameterAdditionalData) oldDataRaw;
            RsParameterAdditionalData newData = (RsParameterAdditionalData) newDataRaw;

            if (oldData.isPatIdent() && newData.isPatIdent() && !oldParam.getName().equals(parameter.getName())) {
                return true;
            }
        }
        return false;
    }
}
