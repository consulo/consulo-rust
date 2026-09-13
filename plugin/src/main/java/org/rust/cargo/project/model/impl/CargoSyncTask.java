/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import org.rust.cargo.project.workspace.StandardLibraryFactory;

import org.rust.cargo.project.workspace.CargoWorkspaceFactory;
import org.rust.cargo.toolchain.RsToolchainLocator;
import consulo.application.AllIcons;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.DefaultBuildDescriptor;
import consulo.build.ui.SyncViewManager;
import consulo.build.ui.event.MessageEvent;
import consulo.build.ui.progress.BuildProgress;
import consulo.build.ui.progress.BuildProgressDescriptor;
import consulo.component.ProcessCanceledException;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.LegacyDumbAwareAction;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.RsTask;
import org.rust.cargo.api.CargoConfig;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.ProcessProgressListener;
import org.rust.cargo.api.model.RustcInfo;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.api.settings.RustProjectSettingsService;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.api.workspace.StandardLibrary;
import org.rust.cargo.project.model.sync.CargoBuildAdapterBase;
import org.rust.cargo.project.model.sync.CargoBuildContextBase;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.api.toolchain.RustcVersion;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.tools.CargoCallType;
import org.rust.cargo.toolchain.tools.ProjectDescription;
import org.rust.cargo.toolchain.tools.ProjectDescriptionStatus;
import org.rust.cargo.toolchain.tools.Rustc;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.cargo.util.DownloadResult;
import org.rust.cargo.util.UnitTestRustcCacheService;
import org.rust.openapiext.RsProcessExecutionException;
import org.rust.openapiext.TaskResult;
import org.rust.stdext.RsResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Reloads the attached Cargo projects: queries the toolchain version, runs {@code cargo metadata} to
 * rebuild the workspace, and attaches the standard library. Progress is reported into the Sync view.
 */
@SuppressWarnings("UnstableApiUsage")
public class CargoSyncTask extends Task.Backgroundable implements RsTask {

    private static final Logger LOG = Logger.getInstance(CargoSyncTask.class);

    private final Project rsProject;
    private final List<CargoProjectImpl> cargoProjects;
    private final CompletableFuture<List<CargoProjectImpl>> result;

    public CargoSyncTask(
        @Nonnull Project project,
        @Nonnull List<CargoProjectImpl> cargoProjects,
        @Nonnull CompletableFuture<List<CargoProjectImpl>> result
    ) {
        super(project, RsBundle.message("progress.title.reloading.cargo.projects"), true);
        this.rsProject = project;
        this.cargoProjects = cargoProjects;
        this.result = result;
    }

    @Nonnull
    @Override
    public RsTask.TaskType getTaskType() {
        return RsTask.TaskType.CARGO_SYNC;
    }

    @Override
    public boolean getRunSyncInUnitTests() {
        return true;
    }

    @Override
    public void run(@Nonnull ProgressIndicator indicator) {
        LOG.info("CargoSyncTask started");
        indicator.setIndeterminate(true);
        long start = System.currentTimeMillis();

        BuildProgress<BuildProgressDescriptor> syncProgress =
            faultTolerant(SyncViewManager.getInstance(rsProject).createBuildProgress());

        List<CargoProjectImpl> refreshedProjects;
        try {
            syncProgress.start(createSyncProgressDescriptor(indicator));
            refreshedProjects = doRun(indicator, syncProgress);
            boolean isUpdateFailed = false;
            for (CargoProjectImpl cargoProject : refreshedProjects) {
                if (cargoProject.getMergedStatus() instanceof CargoProject.UpdateStatus.UpdateFailed) {
                    isUpdateFailed = true;
                    break;
                }
            }
            if (isUpdateFailed) {
                syncProgress.fail();
            } else {
                syncProgress.finish();
            }
        } catch (Throwable e) {
            if (e instanceof ProcessCanceledException) {
                syncProgress.cancel();
            } else {
                syncProgress.fail();
            }
            result.completeExceptionally(e);
            throw e;
        }
        result.complete(refreshedProjects);

        long elapsed = System.currentTimeMillis() - start;
        LOG.debug("Finished Cargo sync task in " + elapsed + " ms");
    }

    /**
     * The sync reports into a build view that not every frontend can show. A view that fails to open or
     * to take an event must not take the sync down with it, so every call is allowed to fail and the
     * chain carries on; only cancellation still propagates.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    private static BuildProgress<BuildProgressDescriptor> faultTolerant(
        @Nonnull BuildProgress<BuildProgressDescriptor> delegate
    ) {
        return (BuildProgress<BuildProgressDescriptor>) java.lang.reflect.Proxy.newProxyInstance(
            CargoSyncTask.class.getClassLoader(),
            new Class<?>[]{BuildProgress.class},
            (proxy, method, args) -> {
                try {
                    Object value = method.invoke(delegate, args);
                    return value == delegate ? proxy : value;
                }
                catch (java.lang.reflect.InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof ProcessCanceledException) throw cause;
                    LOG.warn("Cargo sync view rejected " + method.getName(), cause);
                    return BuildProgress.class.isAssignableFrom(method.getReturnType()) ? proxy : null;
                }
            });
    }

    @Nonnull
    private List<CargoProjectImpl> doRun(
        @Nonnull ProgressIndicator indicator,
        @Nonnull BuildProgress<BuildProgressDescriptor> syncProgress
    ) {
        RsToolchainBase toolchain = RsToolchainLocator.getToolchain(rsProject);
        if (toolchain == null) {
            // Worth saying out loud: without this the sync simply produces nothing and the editor shows
            // every reference unresolved with no explanation.
            LOG.warn("Cargo sync stopped: no module carries a Rust toolchain bundle");
            syncProgress.fail(
                System.currentTimeMillis(),
                LocalizeValue.of(RsBundle.message("build.event.message.cargo.project.update.failed.no.rust.toolchain"))
            );
            return cargoProjects;
        }

        List<CargoProjectWithStdlib> results = new ArrayList<>(cargoProjects.size());
        for (CargoProjectImpl cargoProject : cargoProjects) {
            results.add(runWithChildProgress(
                syncProgress,
                RsBundle.message("build.event.title.sync.project", cargoProject.getPresentableName()),
                childProgress -> {
                    Path workingDirectory = org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(cargoProject);
                    if (!Files.exists(workingDirectory)) {
                        childProgress.message(
                            LocalizeValue.of(RsBundle.message("tooltip.project.directory.does.not.exist")),
                            LocalizeValue.of(RsBundle.message(
                                "build.event.message.project.directory.does.not.exist.consider.detaching.project.from.cargo.tool.window",
                                workingDirectory, cargoProject.getPresentableName())),
                            MessageEvent.Kind.ERROR,
                            null
                        );
                        TaskResult<CargoWorkspace> failed =
                            new TaskResult.Err<>(RsBundle.message("tooltip.project.directory.does.not.exist"));
                        return new CargoProjectWithStdlib(
                            cargoProject.withStdlib(new TaskResult.Err<>(
                                RsBundle.message("tooltip.project.directory.does.not.exist"))),
                            null
                        );
                    }

                    SyncContext context = new SyncContext(
                        rsProject, cargoProject, toolchain, indicator, syncProgress.getId(), childProgress);
                    TaskResult<RustcInfo> rustcInfoResult = fetchRustcInfo(context);
                    RustcInfo rustcInfo = rustcInfoResult instanceof TaskResult.Ok<RustcInfo> ok ? ok.getValue() : null;
                    CargoProjectImpl updated = cargoProject
                        .withRustcInfo(rustcInfoResult)
                        .withWorkspace(fetchCargoWorkspace(context, rustcInfo));
                    return new CargoProjectWithStdlib(updated, fetchStdlib(context, updated, rustcInfo));
                },
                (childProgress, ignored) -> childProgress.finish()
            ));
        }

        return deduplicateProjects(chooseAndAttachStdlib(results));
    }

    @Nonnull
    private BuildProgressDescriptor createSyncProgressDescriptor(@Nonnull ProgressIndicator progress) {
        String basePath = rsProject.getBasePath();
        DefaultBuildDescriptor descriptor = new DefaultBuildDescriptor(
            new Object(),
            LocalizeValue.of(RsBundle.message("build.event.title.cargo")),
            basePath != null ? basePath : "",
            System.currentTimeMillis()
        );
        descriptor.setActivateToolWindowWhenFailed(true);
        descriptor.setActivateToolWindowWhenAdded(false);
        AnAction refreshAction = ActionManager.getInstance().getAction("Cargo.RefreshCargoProject");
        if (refreshAction != null) {
            descriptor.withRestartAction(refreshAction);
        }
        descriptor.withRestartAction(new StopAction(progress));
        return BuildProgressDescriptor.of(descriptor);
    }

    @Nonnull
    private static TaskResult<RustcInfo> fetchRustcInfo(@Nonnull SyncContext context) {
        return context.runWithChildProgress(
            RsBundle.message("progress.text.getting.toolchain.version"),
            childContext -> {
                RsToolchainBase toolchain = childContext.toolchain;
                if (!toolchain.looksLikeValidToolchain()) {
                    return new TaskResult.Err<>(
                        RsBundle.message("invalid.rust.toolchain.02", toolchain.getPresentableLocation()));
                }

                Path workingDirectory = org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(childContext.oldCargoProject);
                Rustc rustc = Rustc.create(toolchain);

                RsResult<RustcVersion, RsProcessExecutionException> versionResult =
                    rustc.queryVersion(workingDirectory, childContext.project, new RustcVersionProcessAdapter(childContext));
                RustcVersion rustcVersion;
                if (versionResult instanceof RsResult.Ok<RustcVersion, RsProcessExecutionException> ok) {
                    rustcVersion = ok.getOk();
                } else {
                    RsProcessExecutionException err = versionResult.err();
                    LOG.warn("Failed to fetch rustc version", err);
                    context.error(
                        RsBundle.message("build.event.title.failed.to.fetch.rustc.version"),
                        err != null && err.getMessage() != null ? err.getMessage() : ""
                    );
                    rustcVersion = null;
                }

                RustcVersion cacheKey = rustcVersion;
                String sysroot = UnitTestRustcCacheService.cached(cacheKey, () -> rustc.getSysroot(workingDirectory));
                if (sysroot == null) {
                    return new TaskResult.Err<>(RsBundle.message("failed.to.get.project.sysroot"));
                }
                String rustupActiveToolchain = UnitTestRustcCacheService.cached(cacheKey, () -> {
                    Rustup rustup = Rustup.create(toolchain, workingDirectory);
                    return rustup == null ? null : rustup.activeToolchainName();
                });
                List<String> rustcTargets = UnitTestRustcCacheService.cached(cacheKey, () -> rustc.getTargets(workingDirectory));

                return new TaskResult.Ok<>(new RustcInfo(sysroot, rustcVersion, rustupActiveToolchain, rustcTargets));
            }
        );
    }

    @Nonnull
    private static TaskResult<CargoWorkspace> fetchCargoWorkspace(@Nonnull SyncContext context, @Nullable RustcInfo rustcInfo) {
        return context.runWithChildProgress(
            RsBundle.message("progress.text.updating.workspace.info"),
            childContext -> {
                RsToolchainBase toolchain = childContext.toolchain;
                if (!toolchain.looksLikeValidToolchain()) {
                    return new TaskResult.Err<>(
                        RsBundle.message("invalid.rust.toolchain.0", toolchain.getPresentableLocation()));
                }
                Path projectDirectory = org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(childContext.oldCargoProject);
                Cargo cargo = Cargo.cargoOrWrapper(toolchain, projectDirectory);
                RustcVersion rustcVersion = rustcInfo != null ? rustcInfo.getVersion() : null;

                RsResult<CargoConfig, ?> cargoConfigResult = UnitTestRustcCacheService.cached(
                    rustcVersion,
                    () -> !Files.exists(projectDirectory.resolve(".cargo")),
                    () -> cargo.getConfig(childContext.project, projectDirectory)
                );
                CargoConfig cargoConfig;
                if (cargoConfigResult instanceof RsResult.Ok<CargoConfig, ?> ok) {
                    cargoConfig = ok.getOk();
                } else {
                    Object err = cargoConfigResult.err();
                    String errMessage = err instanceof Throwable t && t.getMessage() != null ? t.getMessage() : "";
                    childContext.warning(
                        RsBundle.message("build.event.title.fetching.cargo.config"),
                        RsBundle.message("build.event.message.fetching.cargo.config.failed", errMessage)
                    );
                    cargoConfig = CargoConfig.DEFAULT;
                }

                CargoEventService.getInstance(childContext.project).onMetadataCall(projectDirectory);

                List<String> buildTargets = cargoConfig.buildTargets();
                if (buildTargets.isEmpty()) {
                    String host = rustcVersion != null ? rustcVersion.getHost() : null;
                    buildTargets = host != null ? List.of(host) : Collections.emptyList();
                }

                RsResult<ProjectDescription, ?> descriptionResult = cargo.fullProjectDescription(
                    childContext.project,
                    projectDirectory,
                    buildTargets,
                    rustcVersion,
                    callType -> {
                        if (callType == CargoCallType.BUILD_SCRIPT_CHECK) {
                            BuildProgress<BuildProgressDescriptor> buildScriptsProgress = childContext.syncProgress
                                .startChildProgress(LocalizeValue.of(RsBundle.message("build.event.title.build.scripts.evaluation")));
                            SyncContext buildContextOwner = childContext.withSyncProgress(buildScriptsProgress);
                            CargoBuildContextBase buildContext = new SyncCargoBuildContext(
                                childContext.oldCargoProject,
                                buildContextOwner.buildId,
                                buildContextOwner.getId(),
                                buildContextOwner.progress
                            );
                            return new SyncCargoBuildAdapter(buildContextOwner, buildContext);
                        }
                        return new SyncProcessAdapter(childContext);
                    }
                );
                if (!(descriptionResult instanceof RsResult.Ok<ProjectDescription, ?> descriptionOk)) {
                    Object err = descriptionResult.err();
                    String errMessage = err instanceof Throwable t ? t.getMessage() : null;
                    return new TaskResult.Err<>(RsBundle.message("failed.to.run.cargo"), errMessage);
                }
                ProjectDescription description = descriptionOk.getOk();
                if (description.getStatus() == ProjectDescriptionStatus.BUILD_SCRIPT_EVALUATION_ERROR) {
                    childContext.warning(
                        RsBundle.message("build.event.title.build.scripts.evaluation.failed"),
                        RsBundle.message("build.event.message.build.scripts.evaluation.failed.features.based.on.generated.info.by.build.scripts.may.not.work.in.your.ide")
                    );
                }

                Path manifestPath = projectDirectory.resolve("Cargo.toml");

                RsResult<CfgOptions, ?> cfgOptionsResult = UnitTestRustcCacheService.cached(
                    rustcVersion,
                    () -> !Files.exists(projectDirectory.resolve(".cargo")),
                    () -> cargo.getCfgOption(childContext.project, projectDirectory)
                );
                CfgOptions cfgOptions;
                if (cfgOptionsResult instanceof RsResult.Ok<CfgOptions, ?> ok) {
                    cfgOptions = ok.getOk();
                } else {
                    Object err = cfgOptionsResult.err();
                    String errMessage = err instanceof Throwable t && t.getMessage() != null ? t.getMessage() : "";
                    childContext.warning(
                        RsBundle.message("build.event.title.fetching.target.specific.cfg.options"),
                        RsBundle.message("build.event.message.fetching.target.specific.cfg.options.failed.fallback.to.host.options", errMessage)
                    );
                    cfgOptions = Rustc.create(toolchain).getCfgOptions(projectDirectory);
                }

                CargoWorkspace ws = CargoWorkspaceFactory.deserialize(
                    manifestPath, description.getWorkspaceData(), cfgOptions, cargoConfig);
                return new TaskResult.Ok<>(ws);
            }
        );
    }

    @Nonnull
    private static TaskResult<StandardLibrary> fetchStdlib(
        @Nonnull SyncContext context,
        @Nonnull CargoProjectImpl cargoProject,
        @Nullable RustcInfo rustcInfo
    ) {
        return context.runWithChildProgress(
            RsBundle.message("progress.text.getting.rust.stdlib"),
            childContext -> {
                Path workingDirectory = org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(cargoProject);
                if (cargoProject.doesProjectLooksLikeRustc()) {
                    // rust-lang/rust keeps the stdlib inside the project itself
                    StandardLibrary std = StandardLibraryFactory.fromPath(
                        childContext.project,
                        workingDirectory.toString(),
                        rustcInfo,
                        CargoConfig.DEFAULT,
                        true,
                        null
                    );
                    if (std != null) {
                        return new TaskResult.Ok<>(std);
                    }
                }

                CargoWorkspace rawWorkspace = cargoProject.getRawWorkspace();
                CargoConfig cargoConfig = rawWorkspace != null && rawWorkspace.getCargoConfig() != null
                    ? rawWorkspace.getCargoConfig()
                    : CargoConfig.DEFAULT;

                Rustup rustup = Rustup.create(childContext.toolchain, workingDirectory);
                if (rustup == null) {
                    RustProjectSettingsService settings = RsProjectSettingsServiceUtil.getRustSettings(childContext.project);
                    String explicitPath = settings.getExplicitPathToStdlib();
                    if (explicitPath == null) {
                        VirtualFile fromSysroot = Rustc.create(childContext.toolchain).getStdlibFromSysroot(workingDirectory);
                        explicitPath = fromSysroot != null ? fromSysroot.getPath() : null;
                    }
                    if (explicitPath == null) {
                        return new TaskResult.Err<>(RsBundle.message("no.explicit.stdlib.or.rustup.found"));
                    }
                    StandardLibrary lib = StandardLibraryFactory.fromPath(
                        childContext.project, explicitPath, rustcInfo, cargoConfig, false, null);
                    if (lib == null) {
                        return new TaskResult.Err<>(RsBundle.message("invalid.standard.library.0", explicitPath));
                    }
                    return new TaskResult.Ok<>(lib);
                }

                DownloadResult<VirtualFile> download = UnitTestRustcCacheService.cached(
                    rustcInfo != null ? rustcInfo.getVersion() : null,
                    () -> rustup.downloadStdlib(null, null)
                );
                if (download instanceof DownloadResult.Ok<VirtualFile> ok) {
                    StandardLibrary lib = StandardLibraryFactory.fromFile(
                        childContext.project, ok.getValue(), rustcInfo, cargoConfig, false,
                        new SyncProcessAdapter(childContext));
                    if (lib == null) {
                        return new TaskResult.Err<>(
                            RsBundle.message("corrupted.standard.library.0", ok.getValue().getPresentableUrl()));
                    }
                    return new TaskResult.Ok<>(lib);
                }
                return new TaskResult.Err<>(
                    RsBundle.message("download.failed.0", ((DownloadResult.Err<VirtualFile>) download).getError()));
            }
        );
    }

    /**
     * Only one stdlib can be attached at a time. When cargo projects resolve different standard libraries
     * (a {@code rust-toolchain.toml} file or a {@code rustup override}), pick the most recent one, unless
     * one of the projects embeds its own stdlib.
     */
    @Nonnull
    private static List<CargoProjectImpl> chooseAndAttachStdlib(@Nonnull List<CargoProjectWithStdlib> projects) {
        StandardLibrary theMostRecentStdlib = null;
        RustcVersion theMostRecentVersion = null;
        for (CargoProjectWithStdlib it : projects) {
            RustcInfo rustcInfo = it.cargoProject.getRustcInfo();
            if (rustcInfo == null || rustcInfo.getVersion() == null) continue;
            if (!(it.stdlib instanceof TaskResult.Ok<StandardLibrary> ok)) continue;
            if (ok.getValue().isPartOfCargoProject()) {
                theMostRecentStdlib = ok.getValue();
                break;
            }
            if (theMostRecentVersion == null
                || rustcInfo.getVersion().getSemver().compareTo(theMostRecentVersion.getSemver()) > 0) {
                theMostRecentVersion = rustcInfo.getVersion();
                theMostRecentStdlib = ok.getValue();
            }
        }

        List<CargoProjectImpl> result = new ArrayList<>(projects.size());
        for (CargoProjectWithStdlib it : projects) {
            if (it.stdlib == null) {
                result.add(it.cargoProject);
            } else if (it.stdlib instanceof TaskResult.Err) {
                result.add(it.cargoProject.withStdlib(it.stdlib));
            } else if (theMostRecentStdlib != null) {
                result.add(it.cargoProject.withStdlib(new TaskResult.Ok<>(theMostRecentStdlib)));
            } else {
                result.add(it.cargoProject.withStdlib(it.stdlib));
            }
        }
        return result;
    }

    /**
     * Removes duplicated cargo projects. There are two kinds of duplication:
     * <ol>
     *   <li>two projects with the same {@link CargoProject#getManifest()};</li>
     *   <li>a workspace member of one project that is also attached as a separate project — possible
     *       when two independent projects exist and then one becomes a workspace member of the other.</li>
     * </ol>
     * Keep in sync with the existing-project check in {@code CargoProjectsServiceImpl}.
     */
    @Nonnull
    private static List<CargoProjectImpl> deduplicateProjects(@Nonnull List<CargoProjectImpl> projects) {
        List<CargoProjectImpl> distinct = new ArrayList<>();
        Set<Path> seenManifests = new HashSet<>();
        for (CargoProjectImpl project : projects) {
            if (seenManifests.add(project.getManifest())) {
                distinct.add(project);
            }
        }

        // `project root` -> [`package root`] for the workspace packages of that project
        Map<Path, Set<Path>> projectRootToWorkspacePackages = new HashMap<>();
        for (CargoProjectImpl project : distinct) {
            Set<Path> packageRoots = new HashSet<>();
            CargoWorkspace workspace = project.getRawWorkspace();
            if (workspace != null) {
                for (CargoWorkspace.Package pkg : workspace.getPackages()) {
                    if (pkg.getOrigin() == PackageOrigin.WORKSPACE) {
                        packageRoots.add(pkg.getRootDirectory());
                    }
                }
            }
            projectRootToWorkspacePackages.put(project.getManifest().getParent(), packageRoots);
        }

        // Cargo requires a workspace root to be an ancestor of its members in the file hierarchy, so walk
        // the ancestors of each project root and drop the project when an ancestor project already owns it
        // as a workspace package.
        Set<CargoProjectImpl> projectsToRemove = new LinkedHashSet<>();
        for (CargoProjectImpl project : distinct) {
            Path projectRootPath = project.getManifest().getParent();
            if (projectRootPath == null) continue;
            for (Path ancestor = projectRootPath.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
                Set<Path> ancestorPackageRoots = projectRootToWorkspacePackages.get(ancestor);
                if (ancestorPackageRoots != null && ancestorPackageRoots.contains(projectRootPath)) {
                    projectsToRemove.add(project);
                }
            }
        }

        List<CargoProjectImpl> result = new ArrayList<>(distinct.size());
        for (CargoProjectImpl project : distinct) {
            if (!projectsToRemove.contains(project)) {
                result.add(project);
            }
        }
        return result;
    }

    private static <R> R runWithChildProgress(
        @Nonnull BuildProgress<BuildProgressDescriptor> parent,
        @Nonnull String title,
        @Nonnull Function<BuildProgress<BuildProgressDescriptor>, R> action,
        @Nonnull BiConsumer<BuildProgress<BuildProgressDescriptor>, R> onResult
    ) {
        BuildProgress<BuildProgressDescriptor> childProgress = parent.startChildProgress(LocalizeValue.of(title));
        try {
            R actionResult = action.apply(childProgress);
            onResult.accept(childProgress, actionResult);
            return actionResult;
        } catch (Throwable e) {
            if (e instanceof ProcessCanceledException) {
                parent.cancel();
            } else {
                parent.fail();
            }
            throw e;
        }
    }

    private static final class CargoProjectWithStdlib {
        private final CargoProjectImpl cargoProject;
        @Nullable
        private final TaskResult<StandardLibrary> stdlib;

        private CargoProjectWithStdlib(CargoProjectImpl cargoProject, @Nullable TaskResult<StandardLibrary> stdlib) {
            this.cargoProject = cargoProject;
            this.stdlib = stdlib;
        }
    }

    private static class StopAction extends LegacyDumbAwareAction {
        private final ProgressIndicator progress;

        StopAction(@Nonnull ProgressIndicator progress) {
            super(LocalizeValue.localizeTODO("Stop"), LocalizeValue.empty(), AllIcons.Actions.Suspend);
            this.progress = progress;
        }

        @Override
        public void update(@Nonnull AnActionEvent e) {
            e.getPresentation().setEnabled(progress.isRunning());
        }

        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            progress.cancel();
        }
    }

    public static class SyncContext {
        @Nonnull public final Project project;
        @Nonnull public final CargoProjectImpl oldCargoProject;
        @Nonnull public final RsToolchainBase toolchain;
        @Nonnull public final ProgressIndicator progress;
        @Nonnull public final Object buildId;
        @Nonnull public final BuildProgress<BuildProgressDescriptor> syncProgress;

        public SyncContext(
            @Nonnull Project project,
            @Nonnull CargoProjectImpl oldCargoProject,
            @Nonnull RsToolchainBase toolchain,
            @Nonnull ProgressIndicator progress,
            @Nonnull Object buildId,
            @Nonnull BuildProgress<BuildProgressDescriptor> syncProgress
        ) {
            this.project = project;
            this.oldCargoProject = oldCargoProject;
            this.toolchain = toolchain;
            this.progress = progress;
            this.buildId = buildId;
            this.syncProgress = syncProgress;
        }

        @Nonnull
        public Object getId() {
            return syncProgress.getId();
        }

        @Nonnull
        public SyncContext withSyncProgress(@Nonnull BuildProgress<BuildProgressDescriptor> newSyncProgress) {
            return new SyncContext(project, oldCargoProject, toolchain, progress, buildId, newSyncProgress);
        }

        public <T> TaskResult<T> runWithChildProgress(
            @Nonnull String title,
            @Nonnull Function<SyncContext, TaskResult<T>> action
        ) {
            progress.checkCanceled();
            progress.setText(title);
            return CargoSyncTask.runWithChildProgress(
                syncProgress,
                title,
                childProgress -> action.apply(withSyncProgress(childProgress)),
                (childProgress, taskResult) -> {
                    if (taskResult instanceof TaskResult.Err<T> err) {
                        childProgress.message(
                            LocalizeValue.of(err.getReason()),
                            LocalizeValue.of(err.getMessage() != null ? err.getMessage() : ""),
                            MessageEvent.Kind.ERROR,
                            null
                        );
                        childProgress.fail();
                    } else {
                        childProgress.finish();
                    }
                }
            );
        }

        public void withProgressText(@Nonnull String text) {
            progress.setText(text);
            syncProgress.progress(LocalizeValue.of(text));
        }

        public void error(@Nonnull String title, @Nonnull String message) {
            syncProgress.message(LocalizeValue.of(title), LocalizeValue.of(message), MessageEvent.Kind.ERROR, null);
        }

        public void warning(@Nonnull String title, @Nonnull String message) {
            syncProgress.message(LocalizeValue.of(title), LocalizeValue.of(message), MessageEvent.Kind.WARNING, null);
        }
    }

    private static class SyncProcessAdapter extends ProcessAdapter implements ProcessProgressListener {
        private final SyncContext context;

        SyncProcessAdapter(@Nonnull SyncContext context) {
            this.context = context;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
            String text = event.getText().trim();
            if (text.startsWith("Updating") || text.startsWith("Downloading")) {
                context.withProgressText(text);
            }
            if (text.startsWith("Vendoring")) {
                // The vendoring message looks like
                // "Vendoring %package_name% v%package_version% (%src_dir%) to %dst_dir%",
                // so show only the "Vendoring %package_name% v%package_version%" part.
                int index = text.indexOf(" (");
                context.withProgressText(index != -1 ? text.substring(0, index) : text);
            }
        }

        @Override
        public void error(@Nonnull String title, @Nonnull String message) {
            context.error(title, message);
        }

        @Override
        public void warning(@Nonnull String title, @Nonnull String message) {
            context.warning(title, message);
        }
    }

    private static class RustcVersionProcessAdapter extends ProcessAdapter {
        private final SyncContext context;

        RustcVersionProcessAdapter(@Nonnull SyncContext context) {
            this.context = context;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public void onTextAvailable(ProcessEvent event, Key outputType) {
            String text = event.getText().trim();
            if (text.startsWith("info:")) {
                String prefix = RsBundle.message("progress.text.info");
                String stripped = text.startsWith(prefix) ? text.substring(prefix.length()) : text;
                context.withProgressText(stripped.trim());
            }
        }
    }

    private static class SyncCargoBuildContext extends CargoBuildContextBase {
        SyncCargoBuildContext(
            @Nonnull CargoProject cargoProject,
            @Nonnull Object buildId,
            @Nonnull Object parentId,
            @Nonnull ProgressIndicator progressIndicator
        ) {
            super(cargoProject, RsBundle.message("progress.text.building"), false, buildId, parentId);
            setIndicator(progressIndicator);
        }
    }

    private static class SyncCargoBuildAdapter extends CargoBuildAdapterBase {
        private final SyncContext context;

        SyncCargoBuildAdapter(@Nonnull SyncContext context, @Nonnull CargoBuildContextBase buildContext) {
            super(buildContext, SyncViewManager.getInstance(context.project));
            this.context = context;
        }

        @Override
        public void onBuildOutputReaderFinish(ProcessEvent event, boolean isSuccess, boolean isCanceled, Throwable error) {
            if (isSuccess) {
                context.syncProgress.finish();
            } else if (isCanceled) {
                context.syncProgress.cancel();
            } else {
                context.syncProgress.fail();
            }
        }
    }
}
