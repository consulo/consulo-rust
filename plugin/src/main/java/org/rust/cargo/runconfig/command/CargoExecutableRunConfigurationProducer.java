/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import consulo.execution.action.Location;
import consulo.execution.action.ConfigurationContext;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.openapiext.OpenApiUtil;

public class CargoExecutableRunConfigurationProducer extends CargoRunConfigurationProducer {

    @Override
    public boolean isConfigurationFromContext(
        @Nonnull CargoCommandConfiguration configuration,
        @Nonnull ConfigurationContext context
    ) {
        Location<?> location = context.getLocation();
        if (location == null) return false;
        ExecutableTarget target = findBinaryTarget(location);
        if (target == null) return false;
        return configuration.canBeFrom(target.cargoCommandLine);
    }

    @Override
    public boolean setupConfigurationFromContext(
        @Nonnull CargoCommandConfiguration configuration,
        @Nonnull ConfigurationContext context,
        @Nonnull SimpleReference<PsiElement> sourceElement
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
