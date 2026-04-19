/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class CargoExecutableRunConfigurationProducer extends CargoRunConfigurationProducer {

    @Override
    public boolean isConfigurationFromContext(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ConfigurationContext context
    ) {
        Location<?> location = context.getLocation();
        if (location == null) return false;
        ExecutableTarget target = findBinaryTarget(location);
        if (target == null) return false;
        return configuration.canBeFrom(target.cargoCommandLine);
    }

    @Override
    public boolean setupConfigurationFromContext(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ConfigurationContext context,
        @NotNull Ref<PsiElement> sourceElement
    ) {
        Location<?> location = context.getLocation();
        if (location == null) return false;
        ExecutableTarget target = findBinaryTarget(location);
        if (target == null) return false;

        PsiElement element = location.getPsiElement();
        RsFunction fn = RsPsiJavaUtil.ancestorStrict(element, RsFunction.class);
        PsiElement source = (fn != null && isMainFunction(fn)) ? fn : context.getPsiLocation() != null ? context.getPsiLocation().getContainingFile() : null;
        sourceElement.set(source);

        configuration.setName(target.configurationName);
        CargoCommandLine cmd = RunConfigUtil.mergeWithDefault(target.cargoCommandLine, configuration);
        configuration.setFromCmd(cmd);
        return true;
    }

    public static boolean isMainFunction(RsFunction fn) {
        if (!RsFunctionUtil.isMain(fn)) return false;
        CargoWorkspace ws = RsElementUtil.getCargoWorkspace(fn);
        if (ws == null) return false;
        return findBinaryTarget(ws, fn.getContainingFile().getVirtualFile()) != null;
    }

    @Nullable
    private static ExecutableTarget findBinaryTarget(Location<?> location) {
        VirtualFile file = location.getVirtualFile();
        if (file == null) return null;
        PsiFile psiFile = org.rust.openapiext.OpenApiUtil.toPsiFile(file, location.getProject());
        if (!(psiFile instanceof RsFile rsFile)) return null;
        CargoWorkspace ws = rsFile.getCargoWorkspace();
        if (ws == null) return null;
        return findBinaryTarget(ws, file);
    }

    @Nullable
    private static ExecutableTarget findBinaryTarget(CargoWorkspace ws, VirtualFile file) {
        CargoWorkspace.Target target = ws.findTargetByCrateRoot(file);
        if (target == null) return null;
        if (!target.getKind().isBin() && !target.getKind().isExampleBin()) return null;
        return new ExecutableTarget(target);
    }

    private static class ExecutableTarget {
        final String configurationName;
        final CargoCommandLine cargoCommandLine;

        ExecutableTarget(CargoWorkspace.Target target) {
            this.configurationName = "Run " + target.getName();
            this.cargoCommandLine = CargoCommandLine.forTarget(target, "run", java.util.Collections.emptyList());
        }
    }
}
