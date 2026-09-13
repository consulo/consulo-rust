/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;


import consulo.util.lang.StringUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.PackageOrigin;
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
import org.rust.lang.core.psi.ext.impl.*;

public class CargoTestRunConfigurationProducer extends CargoTestRunConfigurationProducerBase {

    @Nonnull
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
    protected boolean isSuitable(@Nonnull PsiElement element) {
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
    private TestConfig createConfigForCargoProject(@Nonnull PsiDirectory dir) {
        Path dirPath = VirtualFileExtUtil.getPathAsPath(dir.getVirtualFile());
        CargoProject cargoProject = RsElementExtUtil.findCargoProject(dir);
        if (cargoProject == null) return null;
        if (!dirPath.equals(org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(cargoProject))) return null;
        return new CargoProjectTestConfig(getCommandName(), dir, cargoProject);
    }

    @Nullable
    private TestConfig createConfigForCargoPackage(@Nonnull PsiDirectory dir) {
        Path dirPath = VirtualFileExtUtil.getPathAsPath(dir.getVirtualFile());
        CargoWorkspace.Package cargoPackage = RsElementExtUtil.findCargoPackage(dir);
        if (cargoPackage == null) return null;
        if (!dirPath.equals(cargoPackage.getRootDirectory()) || cargoPackage.getOrigin() != PackageOrigin.WORKSPACE) return null;
        return new CargoPackageTestConfig(getCommandName(), dir, cargoPackage);
    }

    @Nullable
    private TestConfig createConfigForDocTest(@Nonnull List<PsiElement> elements, boolean climbUp) {
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

    private static boolean hasTestFunction(@Nonnull RsMod mod) {
        return RsItemsOwnerUtil.processExpandedItemsExceptImplsAndUses(mod, item -> {
            if (item instanceof RsFunction && RsFunctionUtil.isTest((RsFunction) item)) return true;
            if (item instanceof RsMod && hasTestFunction((RsMod) item)) return true;
            return false;
        });
    }

    private static class CargoProjectTestConfig implements TestConfig {
        @Nonnull private final String commandName;
        @Nonnull private final PsiDirectory sourceElement;
        @Nonnull private final CargoProject cargoProject;

        CargoProjectTestConfig(@Nonnull String commandName, @Nonnull PsiDirectory sourceElement, @Nonnull CargoProject cargoProject) {
            this.commandName = commandName;
            this.sourceElement = sourceElement;
            this.cargoProject = cargoProject;
        }

        @Nonnull @Override public String getCommandName() { return commandName; }
        @Nonnull @Override public List<CargoWorkspace.Target> getTargets() { return Collections.emptyList(); }
        @Nonnull @Override public String getPath() { return ""; }
        @Override public boolean getExact() { return false; }

        @Nonnull
        @Override
        public String getConfigurationName() {
            return "All " + Utils.capitalized(StringUtil.pluralize(commandName));
        }

        @Nonnull @Override public PsiElement getSourceElement() { return sourceElement; }

        @Nonnull
        @Override
        public CargoCommandLine cargoCommandLine() {
            return CargoCommandLine.forProject(cargoProject, commandName);
        }
    }

    private static class CargoPackageTestConfig implements TestConfig {
        @Nonnull private final String commandName;
        @Nonnull private final PsiDirectory sourceElement;
        @Nonnull private final CargoWorkspace.Package cargoPackage;

        CargoPackageTestConfig(@Nonnull String commandName, @Nonnull PsiDirectory sourceElement, @Nonnull CargoWorkspace.Package cargoPackage) {
            this.commandName = commandName;
            this.sourceElement = sourceElement;
            this.cargoPackage = cargoPackage;
        }

        @Nonnull @Override public String getCommandName() { return commandName; }
        @Nonnull @Override public List<CargoWorkspace.Target> getTargets() { return Collections.emptyList(); }
        @Nonnull @Override public String getPath() { return ""; }
        @Override public boolean getExact() { return false; }

        @Nonnull
        @Override
        public String getConfigurationName() {
            return Utils.capitalized(StringUtil.pluralize(commandName)) + " in '" + sourceElement.getName() + "'";
        }

        @Nonnull @Override public PsiElement getSourceElement() { return sourceElement; }

        @Nonnull
        @Override
        public CargoCommandLine cargoCommandLine() {
            return CargoCommandLine.forPackage(cargoPackage, commandName);
        }
    }

    private static class DocTestConfig implements TestConfig {
        @Nonnull private final String commandName;
        @Nonnull private final String ownerPath;
        @Nonnull private final CargoWorkspace.Target target;
        @Nonnull private final RsDocCodeFence sourceElement;
        @Nonnull private final DocTestContext ctx;

        DocTestConfig(
            @Nonnull String commandName,
            @Nonnull String ownerPath,
            @Nonnull CargoWorkspace.Target target,
            @Nonnull RsDocCodeFence sourceElement,
            @Nonnull DocTestContext ctx
        ) {
            this.commandName = commandName;
            this.ownerPath = ownerPath;
            this.target = target;
            this.sourceElement = sourceElement;
            this.ctx = ctx;
        }

        @Nonnull @Override public String getCommandName() { return commandName; }

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

        @Nonnull
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

        @Nonnull
        @Override
        public List<CargoWorkspace.Target> getTargets() {
            return Collections.singletonList(target);
        }

        @Nonnull
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

        @Nonnull @Override public PsiElement getSourceElement() { return sourceElement; }
    }
}
