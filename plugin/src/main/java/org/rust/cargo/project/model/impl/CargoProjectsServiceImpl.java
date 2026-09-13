/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import org.rust.cargo.api.model.CargoProjectsRefreshListener;
import org.rust.cargo.api.model.MutableUserDisabledFeatures;

import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.model.RustcInfo;
import org.rust.cargo.api.model.UserDisabledFeatures;

import org.rust.cargo.project.model.CargoProjectServiceUtil;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.WriteAction;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.util.indexing.LightDirectoryIndex;
import consulo.language.psi.PsiManager;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.util.lang.SemVer;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jdom.Element;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.*;
import org.rust.cargo.api.settings.RsProjectSettingsServiceBase;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.api.settings.RsSettingsListener;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.FeatureState;
import org.rust.cargo.api.workspace.PackageFeature;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.notifications.NotificationUtils;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.AsyncValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Function;
import consulo.component.ProcessCanceledException;
import consulo.language.editor.DaemonCodeAnalyzer;

/**
 * Keeps the list of {@link CargoProject}s of an IDE project: their manifests, the workspace resolved for
 * each of them, and the indices that map a file back to the project and package that owns it.
 */
@State(name = "CargoProjects", storages = {
    @Storage(StoragePathMacros.WORKSPACE_FILE),
    @Storage(value = "misc.xml", deprecated = true)
})
@ServiceImpl
public class CargoProjectsServiceImpl implements CargoProjectsService, PersistentStateComponent<Element>, Disposable {

    private static final Logger LOG = Logger.getInstance(CargoProjectsServiceImpl.class);

    @Nonnull
    private final Project project;

    /**
     * The heart of the plugin project model. Care must be taken to ensure this is thread-safe,
     * and that refreshes are scheduled after the set of projects changes.
     */
    private final AsyncValue<List<CargoProjectImpl>> projects = new AsyncValue<>(Collections.emptyList());

    /** Sentinel stored for directories that belong to no Cargo project. */
    private final CargoProjectImpl noProjectMarker;

    /** Maps a {@link VirtualFile} to the {@link CargoProject} that contains it. */
    private final LightDirectoryIndex<CargoProjectImpl> directoryIndex;

    private final CargoPackageIndex packageIndex;

    private volatile boolean initialized = false;
    private volatile boolean legacyRustNotificationShowed = false;

    @Inject
    public CargoProjectsServiceImpl(@Nonnull Project project) {
        this.project = project;
        this.noProjectMarker = new CargoProjectImpl(Paths.get(""), this);
        this.directoryIndex = new LightDirectoryIndex<>(project, noProjectMarker, this::fillDirectoryIndex);
        this.packageIndex = new CargoPackageIndex(project, this);

        if (!OpenApiUtil.isUnitTestMode()) {
            MessageBusConnection appConnection = ApplicationManager.getApplication().getMessageBus().connect(project);
            appConnection.subscribe(BulkFileListener.class, new CargoTomlWatcher(this, () -> {
                if (!RsProjectSettingsServiceUtil.getRustSettings(project).getAutoUpdateEnabled()) return;
                refreshAllProjects();
            }));
        }

        MessageBusConnection connection = project.getMessageBus().connect(project);
        connection.subscribe(RsProjectSettingsServiceBase.RUST_SETTINGS_TOPIC, (RsSettingsListener) e -> {
            if (e.getAffectsCargoMetadata()) {
                refreshAllProjects();
            }
        });
    }

    private void fillDirectoryIndex(@Nonnull LightDirectoryIndex<CargoProjectImpl> index) {
        Set<VirtualFile> visited = new HashSet<>();
        List<Map.Entry<CargoWorkspace.Package, CargoProjectImpl>> lowPriority = new ArrayList<>();

        BiConsumer<VirtualFile, CargoProjectImpl> put = (file, cargoProject) -> {
            if (file == null) return;
            if (!visited.add(file)) return;
            index.putInfo(file, cargoProject);
        };

        BiConsumer<CargoWorkspace.Package, CargoProjectImpl> putPackage = (pkg, cargoProject) -> {
            put.accept(pkg.getContentRoot(), cargoProject);
            put.accept(pkg.getOutDir(), cargoProject);
            for (VirtualFile additionalRoot : CargoWorkspace.additionalRoots(pkg)) {
                put.accept(additionalRoot, cargoProject);
            }
            for (CargoWorkspace.Target target : pkg.getTargets()) {
                VirtualFile crateRoot = target.getCrateRoot();
                if (crateRoot != null) {
                    put.accept(crateRoot.getParent(), cargoProject);
                }
            }
        };

        for (CargoProjectImpl cargoProject : projects.getCurrentState()) {
            put.accept(cargoProject.getRootDir(), cargoProject);
            CargoWorkspace workspace = cargoProject.getWorkspace();
            if (workspace == null) continue;
            for (CargoWorkspace.Package pkg : workspace.getPackages()) {
                if (pkg.getOrigin() == PackageOrigin.WORKSPACE) {
                    putPackage.accept(pkg, cargoProject);
                } else {
                    lowPriority.add(Map.entry(pkg, cargoProject));
                }
            }
        }

        for (Map.Entry<CargoWorkspace.Package, CargoProjectImpl> entry : lowPriority) {
            putPackage.accept(entry.getKey(), entry.getValue());
        }
    }

    @Nonnull
    @Override
    public Project getProject() {
        return project;
    }

    @Nonnull
    @Override
    public Collection<CargoProject> getAllProjects() {
        return Collections.unmodifiableList(new ArrayList<>(projects.getCurrentState()));
    }

    @Override
    public boolean getHasAtLeastOneValidProject() {
        return hasAtLeastOneValidProject(projects.getCurrentState());
    }

    @Override
    public boolean getInitialized() {
        return initialized;
    }

    @Nullable
    @Override
    public CargoProject findProjectForFile(@Nonnull VirtualFile file) {
        VirtualFile canonical = file.getCanonicalFile();
        CargoProjectImpl info = directoryIndex.getInfoForFile(file);
        if (info == noProjectMarker && canonical != null && !canonical.equals(file)) {
            info = directoryIndex.getInfoForFile(canonical);
        }
        return info == noProjectMarker ? null : info;
    }

    @Nullable
    @Override
    public CargoWorkspace.Package findPackageForFile(@Nonnull VirtualFile file) {
        CargoWorkspace.Package pkg = packageIndex.findPackageForFile(file);
        if (pkg != null) return pkg;
        VirtualFile canonical = file.getCanonicalFile();
        if (canonical != null && !canonical.equals(file)) {
            return packageIndex.findPackageForFile(canonical);
        }
        return null;
    }

    @Override
    public boolean attachCargoProject(@Nonnull Path manifest) {
        if (isExistingProject(projects.getCurrentState(), manifest)) return false;
        doAttach(manifest);
        return true;
    }

    @Nonnull
    @Override
    public CompletableFuture<?> attachCargoProjectAsync(@Nonnull Path manifest) {
        if (isExistingProject(projects.getCurrentState(), manifest)) {
            return CompletableFuture.completedFuture(projects.getCurrentState());
        }
        return doAttach(manifest);
    }

    @Nonnull
    private CompletableFuture<List<CargoProjectImpl>> doAttach(@Nonnull Path manifest) {
        return modifyProjects(oldProjects -> {
            if (isExistingProject(oldProjects, manifest)) {
                return CompletableFuture.completedFuture(oldProjects);
            }
            List<CargoProjectImpl> newProjects = new ArrayList<>(oldProjects);
            newProjects.add(new CargoProjectImpl(manifest, this));
            return doRefresh(newProjects);
        });
    }

    @Override
    public void attachCargoProjects(@Nonnull Path... manifests) {
        List<Path> candidates = new ArrayList<>();
        for (Path manifest : manifests) {
            if (!isExistingProject(projects.getCurrentState(), manifest)) {
                candidates.add(manifest);
            }
        }
        if (candidates.isEmpty()) return;
        modifyProjects(oldProjects -> {
            List<Path> toAttach = new ArrayList<>();
            for (Path manifest : candidates) {
                if (!isExistingProject(oldProjects, manifest)) {
                    toAttach.add(manifest);
                }
            }
            if (toAttach.isEmpty()) {
                return CompletableFuture.completedFuture(oldProjects);
            }
            List<CargoProjectImpl> newProjects = new ArrayList<>(oldProjects);
            for (Path manifest : toAttach) {
                newProjects.add(new CargoProjectImpl(manifest, this));
            }
            return doRefresh(newProjects);
        });
    }

    @Override
    public void detachCargoProject(@Nonnull CargoProject cargoProject) {
        modifyProjects(oldProjects -> {
            List<CargoProjectImpl> newProjects = new ArrayList<>();
            for (CargoProjectImpl it : oldProjects) {
                if (!it.getManifest().equals(cargoProject.getManifest())) {
                    newProjects.add(it);
                }
            }
            return CompletableFuture.completedFuture(newProjects);
        });
    }

    @Nonnull
    @Override
    public CompletableFuture<? extends List<CargoProject>> refreshAllProjects() {
        return modifyProjects(this::doRefresh).thenApply(ArrayList::new);
    }

    @Nonnull
    @Override
    public CompletableFuture<? extends List<CargoProject>> discoverAndRefresh() {
        VirtualFile guessManifest = null;
        for (VirtualFile candidate : suggestManifests()) {
            guessManifest = candidate;
            break;
        }
        if (guessManifest == null) {
            return CompletableFuture.completedFuture(new ArrayList<CargoProject>(projects.getCurrentState()));
        }
        Path manifestPath = OpenApiUtil.getPathAsPath(guessManifest);
        return modifyProjects(oldProjects -> {
            if (hasAtLeastOneValidProject(oldProjects)) {
                return CompletableFuture.completedFuture(oldProjects);
            }
            return doRefresh(List.of(new CargoProjectImpl(manifestPath, this)));
        }).thenApply(ArrayList::new);
    }

    @Nonnull
    @Override
    public Sequence<VirtualFile> suggestManifests() {
        List<VirtualFile> result = new ArrayList<>();
        for (Module module : OpenApiUtil.getModules(project)) {
            for (VirtualFile contentRoot : ModuleRootManager.getInstance(module).getContentRoots()) {
                VirtualFile manifest = contentRoot.findChild(CargoConstants.MANIFEST_FILE);
                if (manifest != null) {
                    result.add(manifest);
                }
            }
        }
        return result::iterator;
    }

    /**
     * Modifies {@link CargoProject#getUserDisabledFeatures()}, which eventually affects
     * {@link CargoWorkspace.Package#getFeatureState()}. {@link CargoProject} is immutable, so a new
     * instance is created and replaces the old one in {@link #getAllProjects()}.
     */
    @Override
    public void modifyFeatures(@Nonnull CargoProject cargoProject, @Nonnull Set<PackageFeature> features, @Nonnull FeatureState newState) {
        modifyProjectFeatures(cargoProject, (workspace, userDisabledFeatures) -> {
            Map<Path, CargoWorkspace.Package> packagesByRoots = new HashMap<>();
            for (CargoWorkspace.Package pkg : workspace.getPackages()) {
                packagesByRoots.put(pkg.getRootDirectory(), pkg);
            }
            Set<PackageFeature> actualFeatures = new LinkedHashSet<>();
            for (PackageFeature f : features) {
                CargoWorkspace.Package pkg = packagesByRoots.get(f.getPkg().getRootDirectory());
                if (pkg != null) {
                    actualFeatures.add(new PackageFeature(pkg, f.getName()));
                }
            }

            if (newState == FeatureState.Disabled) {
                // Disabling a feature only has to add it to `userDisabledFeatures`; the state of the
                // features that depend on it is inferred in `WorkspaceImpl.inferFeatureState`.
                for (PackageFeature feature : actualFeatures) {
                    userDisabledFeatures.setFeatureState(feature, FeatureState.Disabled);
                }
            } else {
                // Enabling has to keep `userDisabledFeatures` consistent: enabling `f1` requires removing
                // the features `f1` depends on from the disabled set, which would also enable unrelated
                // features that were disabled only because they share a dependency. So recompute the whole
                // disabled set from the feature graph instead of editing single entries.
                Map<PackageFeature, FeatureState> newStates = workspace.getFeatureGraph()
                    .apply(FeatureState.Enabled, view -> {
                        view.disableAll(userDisabledFeatures.getDisabledFeatures(workspace.getPackages()));
                        view.enableAll(actualFeatures);
                    });
                for (Map.Entry<PackageFeature, FeatureState> entry : newStates.entrySet()) {
                    if (entry.getKey().getPkg().getOrigin() == PackageOrigin.WORKSPACE) {
                        userDisabledFeatures.setFeatureState(entry.getKey(), entry.getValue());
                    }
                }
            }
        });
    }

    private void modifyProjectFeatures(
        @Nonnull CargoProject cargoProject,
        @Nonnull BiConsumer<CargoWorkspace, MutableUserDisabledFeatures> action
    ) {
        modifyProjectsLite(oldProjects -> {
            CargoProjectImpl oldProject = null;
            for (CargoProjectImpl it : oldProjects) {
                if (it.getManifest().equals(cargoProject.getManifest())) {
                    if (oldProject != null) return oldProjects;
                    oldProject = it;
                }
            }
            if (oldProject == null) return oldProjects;

            CargoWorkspace workspace = oldProject.getWorkspace();
            if (workspace == null) return oldProjects;

            MutableUserDisabledFeatures userDisabledFeatures = oldProject.getUserDisabledFeatures().toMutable();
            action.accept(workspace, userDisabledFeatures);

            List<CargoProjectImpl> newProjects = new ArrayList<>(oldProjects);
            newProjects.set(newProjects.indexOf(oldProject), oldProject.copy(userDisabledFeatures));
            return newProjects;
        });
    }

    /**
     * All modifications of the project model except the low-level {@link #loadState} go through here:
     * it makes sure that by the time the listeners run, {@link #getAllProjects()} already holds the
     * fresh projects.
     */
    protected CompletableFuture<List<CargoProjectImpl>> modifyProjects(
        @Nonnull Function<List<CargoProjectImpl>, CompletableFuture<List<CargoProjectImpl>>> updater
    ) {
        CargoProjectsRefreshListener refreshStatusPublisher =
            project.getMessageBus().syncPublisher(CargoProjectsService.CARGO_PROJECTS_REFRESH_TOPIC);

        return projects.updateAsync(oldProjects -> {
            refreshStatusPublisher.onRefreshStarted();
            return updater.apply(oldProjects);
        }).thenApply(newProjects -> {
            invokeAndWaitIfNeeded(() -> {
                if (project.isDisposed()) return;
                // Reads versions and may show a balloon - neither wants the write lock.
                if (!newProjects.isEmpty()) {
                    checkRustVersion(newProjects);
                }
                initialized = true;
                publishProjectsChanged(newProjects);
            });
            return newProjects;
        }).handle((newProjects, err) -> {
            CargoRefreshStatus status = err == null ? CargoRefreshStatus.SUCCESS : toRefreshStatus(err);
            refreshStatusPublisher.onRefreshFinished(status);
            return newProjects == null ? projects.getCurrentState() : newProjects;
        });
    }

    @Nonnull
    private static CargoRefreshStatus toRefreshStatus(@Nonnull Throwable err) {
        if (err instanceof ProcessCanceledException) return CargoRefreshStatus.CANCEL;
        if (err instanceof CompletionException && err.getCause() instanceof ProcessCanceledException) {
            return CargoRefreshStatus.CANCEL;
        }
        return CargoRefreshStatus.FAILURE;
    }

    private CompletableFuture<List<CargoProjectImpl>> modifyProjectsLite(
        @Nonnull Function<List<CargoProjectImpl>, List<CargoProjectImpl>> f
    ) {
        return projects.updateSync(f).thenApply(newProjects -> {
            invokeAndWaitIfNeeded(() -> publishProjectsChanged(newProjects));
            return newProjects;
        });
    }

    /**
     * Installs a new project list: drops everything keyed on the old one and tells the editor to
     * re-analyse.
     * <p>
     * Which cargo project a file belongs to decides its crate, and the crate decides which impls
     * resolve for it, so a project list that changes without this is a list the editor never sees -
     * files stay detached and their trait methods stay unresolved.
     * <p>
     * Only the model update takes the write lock. The daemon restart is left outside it: obtaining
     * {@link DaemonCodeAnalyzer} constructs it on first use, and constructing a service inside a
     * write action trips the platform's deadlock guard while the project is still opening.
     */
    private void publishProjectsChanged(@Nonnull List<CargoProjectImpl> newProjects) {
        if (project.isDisposed()) return;
        PsiManager psiManager = PsiManager.getInstance(project);
        WriteAction.run(() -> {
            if (project.isDisposed()) return;
            directoryIndex.resetIndex();
            project.getMessageBus().syncPublisher(CargoProjectsService.CARGO_PROJECTS_TOPIC)
                .cargoProjectsUpdated(this, Collections.unmodifiableList(new ArrayList<>(newProjects)));
            psiManager.dropPsiCaches();
        });
        if (project.isDisposed()) return;
        DaemonCodeAnalyzer.getInstance(project).restart();
    }

    /**
     * The same, scheduled instead of run inline - {@link #loadState} is called while the project is
     * still opening, where taking the write lock is not allowed.
     */
    private void publishProjectsChangedLater(@Nonnull List<CargoProjectImpl> newProjects) {
        ApplicationManager.getApplication().invokeLater(() -> publishProjectsChanged(newProjects));
    }

    private static void invokeAndWaitIfNeeded(@Nonnull Runnable runnable) {
        Application application = ApplicationManager.getApplication();
        if (application.isDispatchThread()) {
            runnable.run();
        } else {
            application.invokeAndWait(runnable);
        }
    }

    private void checkRustVersion(@Nonnull List<CargoProjectImpl> newProjects) {
        SemVer minToolchainVersion = null;
        for (CargoProjectImpl cargoProject : newProjects) {
            RustcInfo info = cargoProject.getRustcInfo();
            if (info == null || info.getVersion() == null) continue;
            SemVer semver = info.getVersion().getSemver();
            if (minToolchainVersion == null || semver.compareTo(minToolchainVersion) < 0) {
                minToolchainVersion = semver;
            }
        }
        if (minToolchainVersion != null && minToolchainVersion.compareTo(RsToolchainBase.MIN_SUPPORTED_TOOLCHAIN) < 0) {
            if (!legacyRustNotificationShowed) {
                String content = RsBundle.message("notification.content.rust.toolchain.no.longer.supported",
                    minToolchainVersion, RsToolchainBase.MIN_SUPPORTED_TOOLCHAIN);
                NotificationUtils.showBalloon(project, content, NotificationType.WARNING);
            }
            legacyRustNotificationShowed = true;
        } else {
            legacyRustNotificationShowed = false;
        }
    }

    private CompletableFuture<List<CargoProjectImpl>> doRefresh(@Nonnull List<CargoProjectImpl> toRefresh) {
        if (toRefresh.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        CompletableFuture<List<CargoProjectImpl>> result = new CompletableFuture<>();
        org.rust.RsProjectTaskQueueService.getInstance(project).run(new CargoSyncTask(project, toRefresh, result));
        return result;
    }

    private static boolean hasAtLeastOneValidProject(@Nonnull Collection<? extends CargoProject> someProjects) {
        for (CargoProject cargoProject : someProjects) {
            if (Files.exists(cargoProject.getManifest())) return true;
        }
        return false;
    }

    /** Keep in sync with the workspace-member deduplication in CargoSyncTask. */
    private static boolean isExistingProject(@Nonnull Collection<? extends CargoProject> someProjects, @Nonnull Path manifest) {
        for (CargoProject cargoProject : someProjects) {
            if (cargoProject.getManifest().equals(manifest)) return true;
        }
        Path parent = manifest.getParent();
        if (parent == null) return false;
        for (CargoProject cargoProject : someProjects) {
            CargoWorkspace workspace = cargoProject.getWorkspace();
            if (workspace == null) continue;
            for (CargoWorkspace.Package pkg : workspace.getPackages()) {
                if (pkg.getOrigin() == PackageOrigin.WORKSPACE && parent.equals(pkg.getRootDirectory())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    @Override
    public Element getState() {
        Element state = new Element("state");
        for (CargoProject cargoProject : getAllProjects()) {
            Element cargoProjectElement = new Element("cargoProject");
            cargoProjectElement.setAttribute("FILE", cargoProject.getManifest().toString().replace('\\', '/'));
            state.addContent(cargoProjectElement);
        }
        return state;
    }

    @Override
    public void loadState(@Nonnull Element state) {
        List<Element> cargoProjects = state.getChildren("cargoProject");
        List<CargoProjectImpl> loaded = new ArrayList<>();

        Map<Path, UserDisabledFeatures> userDisabledFeaturesMap =
            project.getInstance(UserDisabledFeaturesHolder.class).takeLoadedUserDisabledFeatures();

        for (Element cargoProject : cargoProjects) {
            String file = cargoProject.getAttributeValue("FILE");
            if (file == null) continue;
            Path manifest = Paths.get(file);
            UserDisabledFeatures userDisabledFeatures = userDisabledFeaturesMap.getOrDefault(manifest, UserDisabledFeatures.EMPTY);
            loaded.add(new CargoProjectImpl(manifest, this, userDisabledFeatures));
        }

        // Registering the restored projects is all that happens here. Refreshing them needs a toolchain,
        // which comes from the module extension, so the refresh is left to the startup setup that binds
        // the modules first.
        // Only registration happens here. Refreshing needs a toolchain, which is read off the module
        // extensions - and the module model is not loaded yet at this point, so the refresh is left to
        // the startup activity that runs once the project is open.
        projects.updateSync(ignored -> loaded);
        initialized = true;
        // Registering the restored list silently would leave every consumer - the directory index, the
        // crate graph, the PSI caches, the editor notifications - holding the state from before the
        // project existed, and nothing later recomputes them on its own.
        publishProjectsChangedLater(loaded);
    }

    /**
     * Called after {@link #loadState}, and also when there was no state at all (there are no cargo
     * projects yet).
     */
    @Override
    public void afterLoad(boolean first) {
        // Nothing else to do here: in theory `discoverAndRefresh` could run, but the toolchain is most
        // likely not ready yet. Guessing the project model happens in `MissingToolchainNotificationProvider`.
        initialized = true;
        // Initialized together with this service because it stores a part of the cargo project data.
        project.getInstance(UserDisabledFeaturesHolder.class);
    }

    @Override
    public void dispose() {
    }

    @Override
    public String toString() {
        return "CargoProjectsService(projects = " + getAllProjects() + ")";
    }

    public static final String CARGO_DISABLE_PROJECT_REFRESH_ON_CREATION = "cargo.disable.project.refresh.on.creation";
}
