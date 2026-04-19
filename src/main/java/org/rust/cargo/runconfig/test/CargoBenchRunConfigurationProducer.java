/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsModUtil;

import java.util.List;

public class CargoBenchRunConfigurationProducer extends CargoTestRunConfigurationProducerBase {

    @NotNull
    @Override
    protected String getCommandName() {
        return "bench";
    }

    {
        registerConfigProvider((elements, climbUp) -> createConfigForQualifiedElement(elements, climbUp, RsModDeclItem.class));
        registerConfigProvider((elements, climbUp) -> createConfigForQualifiedElement(elements, climbUp, RsFunction.class));
        registerConfigProvider((elements, climbUp) -> createConfigForMod(elements, climbUp));
        registerConfigProvider((elements, climbUp) -> createConfigForMultipleFiles(elements, climbUp));
        registerDirectoryConfigProvider(dir -> createConfigForDirectory(dir));
    }

    @Override
    protected boolean isSuitable(@NotNull PsiElement element) {
        if (!super.isSuitable(element)) return false;
        if (element instanceof RsMod) {
            return hasBenchFunction((RsMod) element);
        } else if (element instanceof RsFunction) {
            return RsFunctionUtil.isBench((RsFunction) element);
        }
        return false;
    }

    private static boolean hasBenchFunction(@NotNull RsMod mod) {
        return RsModUtil.processExpandedItemsExceptImplsAndUses(mod, it -> {
            if (it instanceof RsFunction && RsFunctionUtil.isBench((RsFunction) it)) return true;
            if (it instanceof RsMod && hasBenchFunction((RsMod) it)) return true;
            return false;
        });
    }
}
