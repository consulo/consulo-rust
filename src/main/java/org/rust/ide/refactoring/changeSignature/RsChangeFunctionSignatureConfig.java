/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.changeSignature;

import com.intellij.refactoring.changeSignature.ChangeInfo;
import com.intellij.refactoring.changeSignature.ParameterInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.refactoring.RsFunctionSignatureConfig;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsElement;

public class RsChangeFunctionSignatureConfig extends RsFunctionSignatureConfig {
    @NotNull
    private String myName;
    @NotNull
    private final List<Parameter> myOriginalParameters;
    @Nullable
    private RsTypeReference myReturnTypeDisplay;
    @Nullable
    private RsVis myVisibility;
    private boolean myIsAsync;
    private boolean myIsUnsafe;
    @NotNull
    private final List<Ty> myAdditionalTypesToImport;
    @NotNull
    private final List<Parameter> myParameters;
    @NotNull
    private final String myOriginalName;

    private RsChangeFunctionSignatureConfig(
        @NotNull RsFunction function,
        @NotNull String name,
        @NotNull List<Parameter> originalParameters,
        @Nullable RsTypeReference returnTypeDisplay,
        @Nullable RsVis visibility,
        boolean isAsync,
        boolean isUnsafe,
        @NotNull List<Ty> additionalTypesToImport
    ) {
        super(function);
        myName = name;
        myOriginalParameters = originalParameters;
        myReturnTypeDisplay = returnTypeDisplay;
        myVisibility = visibility;
        myIsAsync = isAsync;
        myIsUnsafe = isUnsafe;
        myAdditionalTypesToImport = additionalTypesToImport;
        myParameters = new ArrayList<>(originalParameters);
        myOriginalName = function.getName() != null ? function.getName() : "";
    }

    @Override
    @NotNull
    protected List<RsTypeParameter> typeParameters() {
        return getFunction().getTypeParameters();
    }

    @NotNull
    public String getName() {
        return myName;
    }

    public void setName(@NotNull String name) {
        myName = name;
    }

    @NotNull
    public List<Parameter> getOriginalParameters() {
        return myOriginalParameters;
    }

    @Nullable
    public RsTypeReference getReturnTypeDisplay() {
        return myReturnTypeDisplay;
    }

    public void setReturnTypeDisplay(@Nullable RsTypeReference returnTypeDisplay) {
        myReturnTypeDisplay = returnTypeDisplay;
    }

    @Nullable
    public RsVis getVisibility() {
        return myVisibility;
    }

    public void setVisibility(@Nullable RsVis visibility) {
        myVisibility = visibility;
    }

    public boolean isAsync() {
        return myIsAsync;
    }

    public void setAsync(boolean isAsync) {
        myIsAsync = isAsync;
    }

    public boolean isUnsafe() {
        return myIsUnsafe;
    }

    public void setUnsafe(boolean isUnsafe) {
        myIsUnsafe = isUnsafe;
    }

    @NotNull
    public List<Ty> getAdditionalTypesToImport() {
        return myAdditionalTypesToImport;
    }

    @NotNull
    public List<Parameter> getParameters() {
        return myParameters;
    }

    @NotNull
    public RsTypeReference getReturnTypeReference() {
        if (myReturnTypeDisplay != null) {
            return myReturnTypeDisplay;
        }
        return new RsPsiFactory(getFunction().getProject()).createType("()");
    }

    public boolean getAllowsVisibilityChange() {
        RsAbstractableOwner owner = RsAbstractableUtil.getOwner(getFunction());
        return !(owner instanceof RsAbstractableOwner.Trait || owner.isTraitImpl());
    }

    @NotNull
    public Ty getReturnType() {
        if (myReturnTypeDisplay != null) {
            return RsTypesUtil.getRawType(myReturnTypeDisplay);
        }
        return TyUnit.INSTANCE;
    }

    @NotNull
    private String getParametersText() {
        List<String> parts = new ArrayList<>();
        RsSelfParameter selfParam = getFunction().getSelfParameter();
        if (selfParam != null) {
            parts.add(selfParam.getText());
        }
        for (Parameter param : myParameters) {
            parts.add(param.getPat().getText() + ": " + param.getTypeReference().getText());
        }
        return String.join(", ", parts);
    }

    @NotNull
    public String signature() {
        StringBuilder sb = new StringBuilder();
        if (myVisibility != null) {
            sb.append(myVisibility.getText()).append(" ");
        }
        if (myIsAsync) {
            sb.append("async ");
        }
        if (myIsUnsafe) {
            sb.append("unsafe ");
        }
        sb.append("fn ").append(myName).append(getTypeParametersText())
            .append("(").append(getParametersText()).append(")");
        if (!(getReturnType() instanceof TyUnit)) {
            sb.append(" -> ").append(getReturnTypeReference().getText());
        }
        sb.append(getWhereClausesText());
        return sb.toString();
    }

    @NotNull
    public ChangeInfo createChangeInfo(boolean changeSignature) {
        return new RsSignatureChangeInfo(this, changeSignature);
    }

    @NotNull
    public ChangeInfo createChangeInfo() {
        return createChangeInfo(true);
    }

    public boolean nameChanged() {
        return !myName.equals(myOriginalName);
    }

    public boolean parameterSetOrOrderChanged() {
        if (myParameters.size() != myOriginalParameters.size()) return true;
        for (int i = 0; i < myParameters.size(); i++) {
            if (myParameters.get(i).getIndex() != i) return true;
        }
        return false;
    }

    @NotNull
    public static RsChangeFunctionSignatureConfig create(@NotNull RsFunction function) {
        RsPsiFactory factory = new RsPsiFactory(function.getProject());
        List<RsValueParameter> rawParams = function.getRawValueParameters();
        List<Parameter> parameters = new ArrayList<>();
        for (int index = 0; index < rawParams.size(); index++) {
            RsValueParameter parameter = rawParams.get(index);
            String patText = parameter.getPat() != null ? parameter.getPat().getText() : "_";
            RsTypeReference parameterCopy = null;
            if (parameter.getTypeReference() != null) {
                parameterCopy = (RsTypeReference) parameter.getTypeReference().copy();
                if (parameter.getTypeReference().getParent() != null
                    && parameter.getTypeReference().getParent().getContext() instanceof RsElement) {
                    RsExpandedElementUtil.setContext(parameterCopy, (RsElement) parameter.getTypeReference().getParent().getContext());
                }
            }
            ParameterProperty<RsTypeReference> type = ParameterProperty.fromItem(parameterCopy);
            parameters.add(new Parameter(factory, patText, type, index));
        }
        return new RsChangeFunctionSignatureConfig(
            function,
            function.getName() != null ? function.getName() : "",
            parameters,
            function.getRetType() != null ? function.getRetType().getTypeReference() : null,
            function.getVis(),
            RsFunctionUtil.isAsync(function),
            function.isUnsafe(),
            new ArrayList<>()
        );
    }
}
