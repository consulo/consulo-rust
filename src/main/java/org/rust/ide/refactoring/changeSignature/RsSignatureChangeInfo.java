/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.changeSignature.ChangeInfo;
import com.intellij.refactoring.changeSignature.ParameterInfo;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;

public class RsSignatureChangeInfo implements ChangeInfo {
    @NotNull
    private final RsChangeFunctionSignatureConfig myConfig;
    private final boolean myChangeSignature;

    public RsSignatureChangeInfo(@NotNull RsChangeFunctionSignatureConfig config, boolean changeSignature) {
        myConfig = config;
        myChangeSignature = changeSignature;
    }

    @NotNull
    public RsChangeFunctionSignatureConfig getConfig() {
        return myConfig;
    }

    public boolean isChangeSignature() {
        return myChangeSignature;
    }

    @NotNull
    @Override
    public ParameterInfo[] getNewParameters() {
        return new ParameterInfo[0];
    }

    @Override
    public boolean isParameterSetOrOrderChanged() {
        return myConfig.parameterSetOrOrderChanged();
    }

    @Override
    public boolean isParameterTypesChanged() {
        return false;
    }

    @Override
    public boolean isParameterNamesChanged() {
        return false;
    }

    @Override
    public boolean isGenerateDelegate() {
        return false;
    }

    @Override
    public boolean isReturnTypeChanged() {
        String configText = myConfig.getReturnTypeDisplay() != null ? myConfig.getReturnTypeDisplay().getText() : null;
        String funcText = myConfig.getFunction().getRetType() != null && myConfig.getFunction().getRetType().getTypeReference() != null
            ? myConfig.getFunction().getRetType().getTypeReference().getText()
            : null;
        return java.util.Objects.equals(configText, funcText);
    }

    @NotNull
    @Override
    public String getNewName() {
        return myConfig.getName();
    }

    @Override
    public boolean isNameChanged() {
        return myConfig.nameChanged();
    }

    @NotNull
    @Override
    public PsiElement getMethod() {
        return myConfig.getFunction();
    }

    @NotNull
    @Override
    public Language getLanguage() {
        return RsLanguage.INSTANCE;
    }
}
