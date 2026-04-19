/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.doc.psi.RsDocCodeFence;
import org.rust.openapiext.VirtualFileExtUtil;
import org.rust.stdext.Utils;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class CargoTestRunConfigurationProducer extends CargoTestRunConfigurationProducerBase {

    @NotNull
    @Override
    protected String getCommandName() {
        return "test";
    }

    {
        registerConfigProvider((elements, climbUp) ->
            createConfigForQualifiedElement(elements, climbUp, RsModDeclItem.class));
        registerConfigProvider((elements, climbUp) ->
            createConfigForDocTest(elements, climbUp));
        registerConfigProvider((elements, climbUp) ->
            createConfigForQualifiedElement(elements, climbUp, RsFunction.class));
        registerConfigProvider((elements, climbUp) ->
            createConfigForMod(elements, climbUp));
        registerConfigProvider((elements, climbUp) ->
            createConfigForMultipleFiles(elements, climbUp));
        registerDirectoryConfigProvider(this::createConfigForDirectory);
        registerDirectoryConfigProvider(this::createConfigForCargoProject);
        registerDirectoryConfigProvider(this::createConfigForCargoPackage);
    }

    @Override
    protected boolean isSuitable(@NotNull PsiElement element) {
        if (!super.isSuitable(element)) return false;
        if (element instanceof RsMod) {
            return hasTestFunction((RsMod) element);
        }
        if (element instanceof RsFunction) {
            return RsFunctionUtil.isTest((RsFunction) element);
        }
        if (element instanceof RsDocCodeFence) {
            return true;
        }
        return false;
    }

    @Nullable
    private TestConfig createConfigForCargoProject(@NotNull PsiDirectory dir) {
        Path dirPath = VirtualFileExtUtil.getPathAsPath(dir.getVirtualFile());
        CargoProject cargoProject = RsElementExtUtil.findCargoProject(dir);
        if (cargoProject == null) return null;
        if (!dirPath.equals(CargoCommandConfiguration.getWorkingDirectory(cargoProject))) return null;
        return new CargoProjectTestConfig(getCommandName(), dir, cargoProject);
    }

    @Nullable
    private TestConfig createConfigForCargoPackage(@NotNull PsiDirectory dir) {
        Path dirPath = VirtualFileExtUtil.getPathAsPath(dir.getVirtualFile());
        CargoWorkspace.Package cargoPackage = RsElementExtUtil.findCargoPackage(dir);
        if (cargoPackage == null) return null;
        if (!dirPath.equals(cargoPackage.getRootDirectory()) || cargoPackage.getOrigin() != PackageOrigin.WORKSPACE) return null;
        return new CargoPackageTestConfig(getCommandName(), dir, cargoPackage);
    }

    @Nullable
    private TestConfig createConfigForDocTest(@NotNull List<PsiElement> elements, boolean climbUp) {
        RsDocCodeFence foundCodeFence = null;
        DocTestContext foundCtx = null;

        for (PsiElement el : elements) {
            RsDocCodeFence originalElement = findElement(el, climbUp, RsDocCodeFence.class);
            if (originalElement == null) continue;
            if (RsElementExtUtil.getContainingCargoTarget(originalElement) == null) continue;

            DocTestContext ctx = DoctestCtxUtil.getDoctestCtx(originalElement);
            if (ctx == null) continue;

            if (!isSuitable(originalElement)) continue;

            if (foundCodeFence != null) {
                // More than one match
                return null;
            }
            foundCodeFence = originalElement;
            foundCtx = ctx;
        }

        if (foundCodeFence == null || foundCtx == null) return null;

        CargoWorkspace.Target target = RsElementExtUtil.getContainingCargoTarget(foundCodeFence);
        if (target == null) return null;
        String ownerPath = configPath(foundCtx.getOwner().getCrateRelativePath());
        if (ownerPath == null) return null;

        return new DocTestConfig(getCommandName(), ownerPath, target, foundCodeFence, foundCtx);
    }

    private static boolean hasTestFunction(@NotNull RsMod mod) {
        return RsItemsOwnerUtil.processExpandedItemsExceptImplsAndUses(mod, item -> {
            if (item instanceof RsFunction && RsFunctionUtil.isTest((RsFunction) item)) return true;
            if (item instanceof RsMod && hasTestFunction((RsMod) item)) return true;
            return false;
        });
    }

    private static class CargoProjectTestConfig implements TestConfig {
        @NotNull private final String commandName;
        @NotNull private final PsiDirectory sourceElement;
        @NotNull private final CargoProject cargoProject;

        CargoProjectTestConfig(@NotNull String commandName, @NotNull PsiDirectory sourceElement, @NotNull CargoProject cargoProject) {
            this.commandName = commandName;
            this.sourceElement = sourceElement;
            this.cargoProject = cargoProject;
        }

        @NotNull @Override public String getCommandName() { return commandName; }
        @NotNull @Override public List<CargoWorkspace.Target> getTargets() { return Collections.emptyList(); }
        @NotNull @Override public String getPath() { return ""; }
        @Override public boolean getExact() { return false; }

        @NotNull
        @Override
        public String getConfigurationName() {
            return "All " + Utils.capitalized(StringUtil.pluralize(commandName));
        }

        @NotNull @Override public PsiElement getSourceElement() { return sourceElement; }

        @NotNull
        @Override
        public CargoCommandLine cargoCommandLine() {
            return CargoCommandLine.forProject(cargoProject, commandName);
        }
    }

    private static class CargoPackageTestConfig implements TestConfig {
        @NotNull private final String commandName;
        @NotNull private final PsiDirectory sourceElement;
        @NotNull private final CargoWorkspace.Package cargoPackage;

        CargoPackageTestConfig(@NotNull String commandName, @NotNull PsiDirectory sourceElement, @NotNull CargoWorkspace.Package cargoPackage) {
            this.commandName = commandName;
            this.sourceElement = sourceElement;
            this.cargoPackage = cargoPackage;
        }

        @NotNull @Override public String getCommandName() { return commandName; }
        @NotNull @Override public List<CargoWorkspace.Target> getTargets() { return Collections.emptyList(); }
        @NotNull @Override public String getPath() { return ""; }
        @Override public boolean getExact() { return false; }

        @NotNull
        @Override
        public String getConfigurationName() {
            return Utils.capitalized(StringUtil.pluralize(commandName)) + " in '" + sourceElement.getName() + "'";
        }

        @NotNull @Override public PsiElement getSourceElement() { return sourceElement; }

        @NotNull
        @Override
        public CargoCommandLine cargoCommandLine() {
            return CargoCommandLine.forPackage(cargoPackage, commandName);
        }
    }

    private static class DocTestConfig implements TestConfig {
        @NotNull private final String commandName;
        @NotNull private final String ownerPath;
        @NotNull private final CargoWorkspace.Target target;
        @NotNull private final RsDocCodeFence sourceElement;
        @NotNull private final DocTestContext ctx;

        DocTestConfig(
            @NotNull String commandName,
            @NotNull String ownerPath,
            @NotNull CargoWorkspace.Target target,
            @NotNull RsDocCodeFence sourceElement,
            @NotNull DocTestContext ctx
        ) {
            this.commandName = commandName;
            this.ownerPath = ownerPath;
            this.target = target;
            this.sourceElement = sourceElement;
            this.ctx = ctx;
        }

        @NotNull @Override public String getCommandName() { return commandName; }

        @Override
        public boolean isIgnored() {
            return ctx.isIgnored();
        }

        // `cargo test` exact matching doesn't work with the escaped spaces
        @Override
        public boolean getExact() {
            return false;
        }

        @Override
        public boolean isDoctest() {
            return true;
        }

        @NotNull
        @Override
        public String getPath() {
            StringBuilder sb = new StringBuilder();
            // `cargo test` uses a regex for matching the test names.
            // Doctests contain spaces in their name (e.g. `foo::bar (line X)`).
            // To make test lookup work, we need to escape spaces in the test path.
            if (!ownerPath.isEmpty()) {
                sb.append(ownerPath).append("\\ ");
            }
            sb.append("(line\\ ").append(ctx.getLineNumber()).append(")");
            return sb.toString();
        }

        @NotNull
        @Override
        public List<CargoWorkspace.Target> getTargets() {
            return Collections.singletonList(target);
        }

        @NotNull
        @Override
        public String getConfigurationName() {
            StringBuilder sb = new StringBuilder();
            sb.append("Doctest of ");
            if (!ownerPath.isEmpty()) {
                sb.append(ownerPath);
            } else {
                // The owner is a crate root library module
                CargoWorkspace.Package pkg = RsElementUtil.getContainingCargoPackage(ctx.getOwner());
                String name = pkg != null ? pkg.getName() : ctx.getOwner().getContainingFile().getName();
                sb.append(name);
            }
            sb.append(" (line ");
            sb.append(ctx.getLineNumber());
            sb.append(")");
            return sb.toString();
        }

        @NotNull @Override public PsiElement getSourceElement() { return sourceElement; }
    }
}
