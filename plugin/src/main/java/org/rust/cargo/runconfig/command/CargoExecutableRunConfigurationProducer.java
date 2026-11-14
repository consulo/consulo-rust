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
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;

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
        VirtualFile file = fn.getContainingFile().getVirtualFile();
        return file != null && findBinaryTarget(fn.getProject(), file) != null;
    }

    @Nullable
    private static ExecutableTarget findBinaryTarget(Location<?> location) {
        VirtualFile file = location.getVirtualFile();
        if (file == null) return null;
        return findBinaryTarget(location.getProject(), file);
    }

    /**
     * The binary whose crate root is {@code file}, found in the Cargo project model by path: a crate root needs no name
     * resolution, and the producers run in action updates, which must not wait for the def maps.
     */
    @Nullable
    private static ExecutableTarget findBinaryTarget(@Nonnull Project project, @Nonnull VirtualFile file) {
        CargoWorkspace.Package pkg = CargoProjectsService.getInstance(project).findPackageForFile(file);
        if (pkg == null) return null;
        CargoWorkspace.Target target = pkg.getWorkspace().findTargetByCrateRoot(file);
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
