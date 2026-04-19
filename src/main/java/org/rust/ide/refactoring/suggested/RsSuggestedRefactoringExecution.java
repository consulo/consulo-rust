/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.suggested;

import com.intellij.openapi.project.Project;
import com.intellij.refactoring.changeSignature.ParameterInfo;
import com.intellij.refactoring.suggested.SuggestedChangeSignatureData;
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution;
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.refactoring.changeSignature.Parameter;
import org.rust.ide.refactoring.changeSignature.ParameterProperty;
import org.rust.ide.refactoring.changeSignature.RsChangeFunctionSignatureConfig;
import org.rust.ide.refactoring.changeSignature.RsChangeSignatureProcessor;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsFunction;

import java.util.ArrayList;
import java.util.List;
import org.rust.lang.core.psi.ext.RsValueParameterUtil;

public class RsSuggestedRefactoringExecution extends SuggestedRefactoringExecution {

    public RsSuggestedRefactoringExecution(@NotNull RsSuggestedRefactoringSupport support) {
        super(support);
    }

    @Nullable
    @Override
    public Object prepareChangeSignature(@NotNull SuggestedChangeSignatureData data) {
        if (!(data.getDeclaration() instanceof RsFunction)) return null;
        RsFunction function = (RsFunction) data.getDeclaration();
        return RsChangeFunctionSignatureConfig.create(function);
    }

    @Override
    public void performChangeSignature(
        @NotNull SuggestedChangeSignatureData data,
        @NotNull List<? extends NewParameterValue> newParameterValues,
        @Nullable Object preparedData
    ) {
        if (!(preparedData instanceof RsChangeFunctionSignatureConfig)) return;
        RsChangeFunctionSignatureConfig config = (RsChangeFunctionSignatureConfig) preparedData;
        if (!(data.getDeclaration() instanceof RsFunction)) return;
        RsFunction function = (RsFunction) data.getDeclaration();
        Project project = function.getProject();

        // At this point, function is restored to its old state
        // We need to create a new config which contains the original function,
        // but which has other attributes set to the modified configuration.
        RsChangeFunctionSignatureConfig originalConfig = RsChangeFunctionSignatureConfig.create(function);

        // We only care about attributes which change triggers the suggested refactoring dialog.
        // Currently it is name and parameters.
        originalConfig.setName(config.getName());

        SuggestedRefactoringSupport.Signature oldSignature = data.getOldSignature();
        SuggestedRefactoringSupport.Signature newSignature = data.getNewSignature();

        // We need to mark "new" parameters with the new parameter index and find parameters swaps.
        int newParameterIndex = 0;
        List<SuggestedRefactoringSupport.Parameter> signatureParameters = newSignature.getParameters();
        List<Parameter> configParameters = originalConfig.getParameters();
        List<Parameter> parameters = new ArrayList<>();

        for (int i = 0; i < signatureParameters.size(); i++) {
            SuggestedRefactoringSupport.Parameter signatureParameter = signatureParameters.get(i);
            Parameter parameter = configParameters.get(i);
            SuggestedRefactoringSupport.Parameter oldParameter = oldSignature.parameterById(signatureParameter.getId());
            boolean isNewParameter = oldParameter == null;

            int index;
            if (oldParameter == null) {
                index = ParameterInfo.NEW_PARAMETER;
            } else {
                index = oldSignature.parameterIndex(oldParameter);
            }

            ParameterProperty<RsExpr> defaultValue;
            if (isNewParameter) {
                NewParameterValue newParameter = newParameterIndex < newParameterValues.size()
                    ? newParameterValues.get(newParameterIndex) : null;
                newParameterIndex++;
                if (newParameter instanceof NewParameterValue.Expression) {
                    RsExpr expr = ((NewParameterValue.Expression) newParameter).getExpression() instanceof RsExpr
                        ? (RsExpr) ((NewParameterValue.Expression) newParameter).getExpression() : null;
                    defaultValue = ParameterProperty.fromItem(expr);
                } else {
                    defaultValue = new ParameterProperty.Empty<>();
                }
            } else {
                defaultValue = new ParameterProperty.Empty<>();
            }

            parameters.add(new Parameter(parameter.getFactory(), parameter.getPatText(), parameter.getType(), index, defaultValue));
        }
        originalConfig.getParameters().clear();
        originalConfig.getParameters().addAll(parameters);

        new RsChangeSignatureProcessor(project, originalConfig.createChangeInfo(false)).run();
    }
}
