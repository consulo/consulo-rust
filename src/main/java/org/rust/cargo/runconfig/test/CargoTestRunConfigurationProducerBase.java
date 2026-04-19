/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.impl.CargoProjectImpl;
import org.rust.cargo.project.model.impl.CargoProjectsServiceImplUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.command.CargoRunConfigurationProducer;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.ide.refactoring.RsNamesValidator;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.RsRawIdentifiers;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.VirtualFileExtUtil;
import org.rust.stdext.Utils;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class CargoTestRunConfigurationProducerBase extends CargoRunConfigurationProducer {
    @NotNull
    protected abstract String getCommandName();

    private final List<BiFunction<List<PsiElement>, Boolean, TestConfig>> testConfigProviders = new ArrayList<>();

    @Override
    public boolean isConfigurationFromContext(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ConfigurationContext context
    ) {
        TestConfig testConfig = findTestConfig(context);
        if (testConfig == null) return false;
        return configuration.canBeFrom(testConfig.cargoCommandLine());
    }

    @Override
    public boolean setupConfigurationFromContext(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ConfigurationContext context,
        @NotNull Ref<PsiElement> sourceElement
    ) {
        TestConfig testConfig = findTestConfig(context);
        if (testConfig == null) return false;
        sourceElement.set(testConfig.getOriginalElement());
        configuration.setName(testConfig.getConfigurationName());
        CargoCommandLine cmd = RunConfigUtil.mergeWithDefault(testConfig.cargoCommandLine(), configuration);
        configuration.setFromCmd(cmd);
        return true;
    }

    protected void registerConfigProvider(@NotNull BiFunction<List<PsiElement>, Boolean, TestConfig> provider) {
        testConfigProviders.add(provider);
    }

    protected void registerDirectoryConfigProvider(@NotNull Function<PsiDirectory, TestConfig> provider) {
        testConfigProviders.add((elements, climbUp) -> {
            if (elements.size() != 1) return null;
            PsiElement single = elements.get(0);
            if (!(single instanceof PsiDirectory)) return null;
            return provider.apply((PsiDirectory) single);
        });
    }

    @Nullable
    public TestConfig findTestConfig(@NotNull List<PsiElement> elements, boolean climbUp) {
        for (BiFunction<List<PsiElement>, Boolean, TestConfig> provider : testConfigProviders) {
            TestConfig config = provider.apply(elements, climbUp);
            if (config != null) return config;
        }
        return null;
    }

    @Nullable
    public TestConfig findTestConfig(@NotNull List<PsiElement> elements) {
        return findTestConfig(elements, true);
    }

    @Nullable
    private TestConfig findTestConfig(@NotNull ConfigurationContext context) {
        PsiElement[] array = LangDataKeys.PSI_ELEMENT_ARRAY.getData(context.getDataContext());
        List<PsiElement> elements;
        if (array != null) {
            elements = Arrays.asList(array);
        } else if (context.getLocation() != null && context.getLocation().getPsiElement() != null) {
            elements = Collections.singletonList(context.getLocation().getPsiElement());
        } else {
            elements = null;
        }
        return elements != null ? findTestConfig(elements) : null;
    }

    @Nullable
    protected TestConfig createConfigForDirectory(@NotNull PsiDirectory dir) {
        List<RsFile> dirTargets = getDirectoryTargets(dir);
        TestConfig filesConfig = createConfigForMultipleFiles(new ArrayList<>(dirTargets), false);
        if (filesConfig == null) return null;

        PsiDirectory sourceRoot = getSourceRoot(dir);
        if (sourceRoot == null) return null;
        String suitableSourceRootName = StringUtil.pluralize(getCommandName());
        if (!suitableSourceRootName.equals(sourceRoot.getName())) return null;

        return new DirectoryTestConfig(getCommandName(), filesConfig.getTargets(), dir);
    }

    @Nullable
    protected TestConfig createConfigForMultipleFiles(@NotNull List<PsiElement> elements, boolean climbUp) {
        List<TestConfig> modConfigs = new ArrayList<>();
        for (PsiElement el : elements) {
            TestConfig config = createConfigForMod(Collections.singletonList(el), climbUp);
            if (config != null) {
                modConfigs.add(config);
            }
        }

        if (modConfigs.size() == 1) {
            return modConfigs.get(0);
        }

        List<CargoWorkspace.Target> targets = modConfigs.stream()
            .flatMap(c -> c.getTargets().stream())
            .collect(Collectors.toList());
        if (targets.size() <= 1) return null;

        // If the selection spans more than one package, bail out.
        Set<CargoWorkspace.Package> packages = targets.stream()
            .map(CargoWorkspace.Target::getPkg)
            .collect(Collectors.toSet());
        if (packages.size() > 1) return null;

        return new MultipleFileTestConfig(getCommandName(), targets, modConfigs.get(0).getSourceElement());
    }

    @Nullable
    protected TestConfig createConfigForQualifiedElement(
        @NotNull List<PsiElement> elements,
        boolean climbUp,
        @NotNull Class<? extends RsQualifiedNamedElement> targetClass
    ) {
        RsQualifiedNamedElement foundOriginal = null;
        RsQualifiedNamedElement foundSource = null;

        for (PsiElement el : elements) {
            RsQualifiedNamedElement originalElement = findElement(el, climbUp, targetClass);
            if (originalElement == null) continue;
            RsQualifiedNamedElement sourceElement = getSourceElement(originalElement);
            if (sourceElement == null) continue;
            if (!isSuitable(sourceElement)) continue;
            if (RsElementExtUtil.getContainingCargoTarget(sourceElement) == null) continue;

            if (foundOriginal != null) {
                // More than one match
                return null;
            }
            foundOriginal = originalElement;
            foundSource = sourceElement;
        }

        if (foundOriginal == null || foundSource == null) return null;

        CargoWorkspace.Target target = RsElementExtUtil.getContainingCargoTarget(foundSource);
        if (target == null) return null;
        String configPath = configPath(foundSource.getCrateRelativePath());
        if (configPath == null) return null;
        return new SingleItemTestConfig(
            getCommandName(), configPath, target, foundSource, foundOriginal,
            isIgnoredTest(foundSource)
        );
    }

    @Nullable
    protected TestConfig createConfigForMod(@NotNull List<PsiElement> elements, boolean climbUp) {
        return createConfigForQualifiedElement(elements, climbUp, RsMod.class);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    protected <T extends PsiElement> T findElement(@NotNull PsiElement base, boolean climbUp, @NotNull Class<T> targetClass) {
        if (targetClass.isInstance(base)) return (T) base;
        if (!climbUp) return null;
        return com.intellij.psi.util.PsiTreeUtil.getParentOfType(base, targetClass, false);
    }

    protected boolean isSuitable(@NotNull PsiElement element) {
        if (!(element instanceof RsElement)) return false;
        Crate crate = RsElementUtil.getContainingCrate(element);
        Crate notFake = Crate.asNotFake(crate);
        return notFake != null && notFake.getOrigin() == PackageOrigin.WORKSPACE;
    }

    protected boolean isIgnoredTest(@NotNull PsiElement element) {
        if (!(element instanceof RsFunction)) return false;
        return RsDocAndAttributeOwnerUtil.findOuterAttr((RsFunction) element, "ignore") != null;
    }

    @Nullable
    public static String configPath(@Nullable String crateRelativePath) {
        if (crateRelativePath == null) return null;
        String path = crateRelativePath;
        if (path.startsWith("::")) {
            path = path.substring(2);
        }
        String[] parts = path.split("::");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append("::");
            sb.append(escapePathFragment(parts[i]));
        }
        return sb.toString();
    }

    private static String escapePathFragment(@NotNull String fragment) {
        if (RsNamesValidator.isKeyword(fragment)) {
            return RsRawIdentifiers.RS_RAW_PREFIX + fragment;
        }
        return fragment;
    }

    @Nullable
    public static RsQualifiedNamedElement getSourceElement(@NotNull RsQualifiedNamedElement element) {
        if (element instanceof RsModDeclItem) {
            PsiElement resolved = ((RsModDeclItem) element).getReference().resolve();
            return resolved instanceof RsQualifiedNamedElement ? (RsQualifiedNamedElement) resolved : null;
        }
        return element;
    }

    @NotNull
    private static List<RsFile> getDirectoryTargets(@NotNull PsiDirectory dir) {
        Path rootPath = VirtualFileExtUtil.getPathAsPath(dir.getVirtualFile());
        return CargoProjectsServiceImplUtil.getAllTargets(CargoProjectServiceUtil.getCargoProjects(dir.getProject())).stream()
            .filter(t -> t.getPkg().getOrigin() == PackageOrigin.WORKSPACE)
            .map(CargoWorkspace.Target::getCrateRoot)
            .filter(Objects::nonNull)
            .distinct()
            .filter(vf -> VirtualFileExtUtil.getPathAsPath(vf).startsWith(rootPath))
            .map(vf -> org.rust.openapiext.PsiFileExtUtil.toPsiFile(vf, dir.getProject()))
            .filter(pf -> pf instanceof RsFile)
            .map(pf -> (RsFile) pf)
            .collect(Collectors.toList());
    }

    @Nullable
    private static PsiDirectory getSourceRoot(@NotNull PsiDirectory dir) {
        // Walk up to find source root
        PsiDirectory current = dir;
        while (current != null) {
            if (current.getParent() != null && isSourceRoot(current)) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    private static boolean isSourceRoot(@NotNull PsiDirectory dir) {
        // Check if this directory is a source root (e.g., "tests", "benches")
        String name = dir.getName();
        return "tests".equals(name) || "benches".equals(name) || "examples".equals(name) || "src".equals(name);
    }

    private static class DirectoryTestConfig implements TestConfig {
        @NotNull private final String commandName;
        @NotNull private final List<CargoWorkspace.Target> targets;
        @NotNull private final PsiDirectory sourceElement;

        DirectoryTestConfig(
            @NotNull String commandName,
            @NotNull List<CargoWorkspace.Target> targets,
            @NotNull PsiDirectory sourceElement
        ) {
            this.commandName = commandName;
            this.targets = targets;
            this.sourceElement = sourceElement;
        }

        @NotNull @Override public String getCommandName() { return commandName; }
        @NotNull @Override public String getPath() { return ""; }
        @Override public boolean getExact() { return false; }
        @NotNull @Override public List<CargoWorkspace.Target> getTargets() { return targets; }

        @NotNull
        @Override
        public String getConfigurationName() {
            return Utils.capitalized(StringUtil.pluralize(commandName)) + " in '" + sourceElement.getName() + "'";
        }

        @NotNull @Override public PsiElement getSourceElement() { return sourceElement; }
    }

    private static class MultipleFileTestConfig implements TestConfig {
        @NotNull private final String commandName;
        @NotNull private final List<CargoWorkspace.Target> targets;
        @NotNull private final PsiElement sourceElement;

        MultipleFileTestConfig(
            @NotNull String commandName,
            @NotNull List<CargoWorkspace.Target> targets,
            @NotNull PsiElement sourceElement
        ) {
            this.commandName = commandName;
            this.targets = targets;
            this.sourceElement = sourceElement;
        }

        @NotNull @Override public String getCommandName() { return commandName; }
        @NotNull @Override public String getPath() { return ""; }
        @Override public boolean getExact() { return false; }
        @NotNull @Override public List<CargoWorkspace.Target> getTargets() { return targets; }

        @NotNull
        @Override
        public String getConfigurationName() {
            return Utils.capitalized(commandName) + " multiple selected files";
        }

        @NotNull @Override public PsiElement getSourceElement() { return sourceElement; }
    }
}
