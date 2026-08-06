/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;
import org.rust.lang.RsLanguage;
import consulo.language.Language;

import consulo.execution.lineMarker.ExecutorAction;
import consulo.execution.lineMarker.RunLineMarkerContributor;
import consulo.ui.ex.action.AnAction;
import consulo.application.AllIcons;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }

    @Nullable
    @Override
    public Info getInfo(@Nonnull PsiElement element) {
        if (RsElementUtil.getElementType(element) != RsElementTypes.IDENTIFIER) return null;
        if (!(element.getParent() instanceof RsFunction)) return null;
        RsFunction fn = (RsFunction) element.getParent();
        if (!CargoExecutableRunConfigurationProducer.isMainFunction(fn)) return null;

        AnAction[] actions = ExecutorAction.getActions(0);
        return new Info(
            AllIcons.RunConfigurations.Application,
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
