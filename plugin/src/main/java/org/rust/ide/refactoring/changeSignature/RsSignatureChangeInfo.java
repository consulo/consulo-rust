/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.changeSignature.ChangeInfo;
import consulo.language.editor.refactoring.changeSignature.ParameterInfo;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;

public class RsSignatureChangeInfo implements ChangeInfo {
    @Nonnull
    private final RsChangeFunctionSignatureConfig myConfig;
    private final boolean myChangeSignature;

    public RsSignatureChangeInfo(@Nonnull RsChangeFunctionSignatureConfig config, boolean changeSignature) {
        myConfig = config;
        myChangeSignature = changeSignature;
    }

    @Nonnull
    public RsChangeFunctionSignatureConfig getConfig() {
        return myConfig;
    }

    public boolean isChangeSignature() {
        return myChangeSignature;
    }

    @Nonnull
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

    @Nonnull
    @Override
    public String getNewName() {
        return myConfig.getName();
    }

    @Override
    public boolean isNameChanged() {
        return myConfig.nameChanged();
    }

    @Nonnull
    @Override
    public PsiElement getMethod() {
        return myConfig.getFunction();
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return RsLanguage.INSTANCE;
    }
}
