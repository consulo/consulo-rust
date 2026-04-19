/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.runconfig.command.CargoExecutableRunConfigurationProducer;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class CargoExecutableRunLineMarkerContributor extends RunLineMarkerContributor {

    @Nullable
    @Override
    public Info getInfo(@NotNull PsiElement element) {
        if (RsElementUtil.getElementType(element) != RsElementTypes.IDENTIFIER) return null;
        if (!(element.getParent() instanceof RsFunction)) return null;
        RsFunction fn = (RsFunction) element.getParent();
        if (!CargoExecutableRunConfigurationProducer.isMainFunction(fn)) return null;

        AnAction[] actions = ExecutorAction.getActions(0);
        return new Info(
            AllIcons.RunConfigurations.TestState.Run,
            psiElement -> {
                String[] texts = Arrays.stream(actions)
                    .map(action -> getText(action, psiElement))
                    .filter(Objects::nonNull)
                    .toArray(String[]::new);
                if (OpenApiUtil.isUnitTestMode()) {
                    return texts.length > 0 ? texts[0] : "";
                } else {
                    return String.join("\n", texts);
                }
            },
            actions
        );
    }
}
