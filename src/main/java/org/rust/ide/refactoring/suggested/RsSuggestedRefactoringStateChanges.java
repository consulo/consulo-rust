/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.suggested;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.suggested.SuggestedRefactoringState;
import com.intellij.refactoring.suggested.SuggestedRefactoringStateChanges;
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPatIdent;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RsSuggestedRefactoringStateChanges extends SuggestedRefactoringStateChanges {

    public RsSuggestedRefactoringStateChanges(@NotNull RsSuggestedRefactoringSupport support) {
        super(support);
    }

    @NotNull
    @Override
    public List<TextRange> parameterMarkerRanges(@NotNull PsiElement anchor) {
        if (!(anchor instanceof RsFunction)) return Collections.emptyList();
        RsFunction function = (RsFunction) anchor;
        List<RsValueParameter> parameters = RsFunctionUtil.getRawValueParameters(function);
        List<TextRange> result = new ArrayList<>();
        for (RsValueParameter parameter : parameters) {
            PsiElement colon = findChildWithElementType(parameter, RsElementTypes.COLON);
            if (colon != null) {
                result.add(colon.getTextRange());
            }
        }
        return result;
    }

    @Nullable
    @Override
    public SuggestedRefactoringSupport.Signature signature(
        @NotNull PsiElement anchor,
        @Nullable SuggestedRefactoringState prevState
    ) {
        if (anchor instanceof RsFunction) {
            RsFunction function = (RsFunction) anchor;
            String name = function.getName();
            if (name == null) return null;
            String returnType = function.getRetType() != null ? function.getRetType().getText() : null;

            List<RsValueParameter> functionParameters = RsFunctionUtil.getRawValueParameters(function);
            // Ignore signatures with invalid parameters
            for (RsValueParameter param : functionParameters) {
                if (param.getPat() == null || param.getTypeReference() == null) return null;
            }

            List<SuggestedRefactoringSupport.Parameter> parameters = new ArrayList<>();
            for (RsValueParameter param : functionParameters) {
                String patText = param.getPat() != null ? param.getPat().getText() : "";
                String typeText = param.getTypeReference() != null ? param.getTypeReference().getText() : "";
                boolean isPatIdent = param.getPat() instanceof RsPatIdent;
                parameters.add(new SuggestedRefactoringSupport.Parameter(
                    new Object(),
                    patText,
                    typeText,
                    new RsParameterAdditionalData(isPatIdent)
                ));
            }
            SuggestedRefactoringSupport.Signature signature = SuggestedRefactoringSupport.Signature.create(
                name, returnType, parameters, new RsSignatureAdditionalData(true)
            );
            if (signature == null) return null;
            if (prevState == null) {
                return signature;
            } else {
                return matchParametersWithPrevState(signature, anchor, prevState);
            }
        } else {
            if (!(anchor instanceof PsiNamedElement)) return null;
            String name = ((PsiNamedElement) anchor).getName();
            if (name == null) return null;
            return SuggestedRefactoringSupport.Signature.create(
                name, null, Collections.emptyList(), new RsSignatureAdditionalData(false)
            );
        }
    }

    @Nullable
    private static PsiElement findChildWithElementType(@NotNull PsiElement parent, @NotNull IElementType type) {
        for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (PsiUtilCore.getElementType(child) == type) {
                return child;
            }
        }
        return null;
    }
}
