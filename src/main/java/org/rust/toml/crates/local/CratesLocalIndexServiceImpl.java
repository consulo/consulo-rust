/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.io.*;
import com.intellij.util.ui.UIUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.RsBundle;
import org.rust.openapiext.RsPathManager;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;
import org.rust.util.RsBackgroundTaskQueue;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CratesLocalIndexServiceImpl implements CratesLocalIndexService, Disposable {

    private static final Logger LOG = Logger.getInstance(CratesLocalIndexServiceImpl.class);
    private static final String CRATES_IO_HASH = "github.com-1ecc6299db9ec823";
    private static final String CORRUPTION_MARKER_NAME = "corruption.marker";
    private static final String CARGO_REGISTRY_INDEX_TAG = "origin/HEAD";
    private static final String INVALID_COMMIT_HASH = "<invalid>";
    private static final int CRATES_INDEX_VERSION = 1;

    private final RsBackgroundTaskQueue myQueue = new RsBackgroundTaskQueue();
    private final Object myInnerStateLock = new Object();
    private volatile InnerState myInnerState = new InnerState.Loading();
    private final AtomicInteger myUpdateTaskCount = new AtomicInteger(0);

    private boolean isUpdating() {
        return myUpdateTaskCount.get() != 0;
    }

    public CratesLocalIndexServiceImpl() {
        loadAsync();
    }

    private void loadAsync() {
        LOG.debug("Loading CratesLocalIndexService");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                resetIndexIfNeeded();
            } catch (IOException e) {
                LOG.error(e);
            }

            RsResult<CratesLocalIndexServiceImplInner, CratesLocalIndexService.Error.InternalError> inner =
                CratesLocalIndexServiceImplInner.tryCreate(getCargoRegistryIndexPath(), getBaseCratesLocalIndexDir());

            synchronized (myInnerStateLock) {
                InnerState oldState = myInnerState;
                if (oldState instanceof InnerState.Loading) {
                    if (inner.isOk()) {
                        CratesLocalIndexServiceImplInner ok = inner.unwrap();
                        updateIndex(ok);
                        myInnerState = new InnerState.Loaded(ok);
                    } else {
                        myInnerState = new InnerState.Err(((RsResult.Err<?, CratesLocalIndexService.Error.InternalError>) inner).getErr());
                    }
                } else if (oldState instanceof InnerState.Disposed) {
                    if (inner.isOk()) {
                        inner.unwrap().close();
                    }
                }
            }
            LOG.debug("Loading CratesLocalIndexService finished");
        });
    }

    @NotNull
    @Override
    public RsResult<CargoRegistryCrate, Error> getCrate(@NotNull String crateName) {
        return handleErrors(inner -> inner.getCrate(crateName));
    }

    @NotNull
    @Override
    public RsResult<List<String>, Error> getAllCrateNames() {
        return handleErrors(CratesLocalIndexServiceImplInner::getAllCrateNames);
    }

    @NotNull
    private <T> RsResult<T, Error> handleErrors(@NotNull ThrowingFunction<CratesLocalIndexServiceImplInner, T> action) {
        if (isUpdating()) {
            return new RsResult.Err<>(Error.UPDATING);
        }
        InnerState state = myInnerState;
        if (state instanceof InnerState.Loading) {
            return new RsResult.Err<>(Error.NOT_YET_LOADED);
        } else if (state instanceof InnerState.Loaded) {
            try {
                return new RsResult.Ok<>(action.apply(((InnerState.Loaded) state).myInner));
            } catch (IOException e) {
                return new RsResult.Err<>(onPersistentHashMapError((InnerState.Loaded) state, e));
            }
        } else if (state instanceof InnerState.Err) {
            return new RsResult.Err<>(((InnerState.Err) state).myErr);
        } else {
            return new RsResult.Err<>(Error.DISPOSED);
        }
    }

    @NotNull
    private Error.InternalError onPersistentHashMapError(@NotNull InnerState.Loaded lastInnerState, @NotNull IOException e) {
        Error.InternalError err = new Error.InternalError.PersistentHashMapReadError(e.toString());
        synchronized (myInnerStateLock) {
            if (myInnerState == lastInnerState) {
                lastInnerState.myInner.close();
                myInnerState = new InnerState.Err(err);
            }
        }
        LOG.warn(e);
        return err;
    }

    public void recoverIfNeeded() {
        boolean shouldReload;
        synchronized (myInnerStateLock) {
            if (myInnerState instanceof InnerState.Err) {
                myInnerState = new InnerState.Loading();
                shouldReload = true;
            } else {
                shouldReload = false;
            }
        }
        if (shouldReload) {
            loadAsync();
        }
    }

    public boolean hasInterestingEvent(@NotNull List<? extends VFileEvent> events) {
        InnerState state = myInnerState;
        if (!(state instanceof InnerState.Loaded)) return false;
        String refsLocation = ((InnerState.Loaded) state).myInner.getCargoRegistryIndexRefsLocation();
        for (VFileEvent event : events) {
            if (event.getPath().startsWith(refsLocation)) return true;
        }
        return false;
    }

    public void updateIndex() {
        InnerState state = myInnerState;
        if (!(state instanceof InnerState.Loaded)) return;
        updateIndex(((InnerState.Loaded) state).myInner);
    }

    private void updateIndex(@NotNull CratesLocalIndexServiceImplInner inner) {
        myUpdateTaskCount.incrementAndGet();
        myQueue.run(inner.createUpdateTask(
            (i, err) -> updateTaskFailed(i, err),
            () -> updateTaskFinished()
        ));
    }

    @Override
    public void dispose() {
        myQueue.dispose();
        synchronized (myInnerStateLock) {
            InnerState state = myInnerState;
            if (state instanceof InnerState.Loaded) {
                ((InnerState.Loaded) state).myInner.close();
            }
            myInnerState = new InnerState.Disposed();
        }
    }

    private void updateTaskFailed(@NotNull CratesLocalIndexServiceImplInner inner, @NotNull Error.InternalError err) {
        synchronized (myInnerStateLock) {
            if (myInnerState instanceof InnerState.Loaded && ((InnerState.Loaded) myInnerState).myInner == inner) {
                myInnerState = new InnerState.Err(err);
            }
        }
    }

    private void updateTaskFinished() {
        WriteAction.run(() -> myUpdateTaskCount.decrementAndGet());
    }

    @TestOnly
    public void awaitLoadedAndUpdated() {
        while (myInnerState instanceof InnerState.Loading || isUpdating()) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            UIUtil.dispatchAllInvocationEvents();
        }
    }

    @NotNull
    private static Path getBaseCratesLocalIndexDir() {
        return RsPathManager.pluginDirInSystem().resolve("crates-local-index");
    }

    @NotNull
    private static Path getCorruptionMarkerFile() {
        return getBaseCratesLocalIndexDir().resolve(CORRUPTION_MARKER_NAME);
    }

    @NotNull
    private static String getCargoHome() {
        String cargoHome = EnvironmentUtil.getValue("CARGO_HOME");
        if (cargoHome != null) return cargoHome;
        return Paths.get(System.getProperty("user.home"), ".cargo/").toString();
    }

    @NotNull
    private static Path getCargoRegistryIndexPath() {
        return Paths.get(getCargoHome(), "registry/index/", CRATES_IO_HASH, ".git/");
    }

    public static void invalidateCaches() throws IOException {
        Path marker = getCorruptionMarkerFile();
        Path parent = marker.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.createFile(marker);
    }

    public static void resetIndexIfNeeded() throws IOException {
        if (Files.exists(getCorruptionMarkerFile())) {
            org.rust.stdext.PathUtil.cleanDirectory(getBaseCratesLocalIndexDir());
        }
    }

    // Inner state classes
    private static abstract class InnerState {
        static class Loading extends InnerState {}
        static class Loaded extends InnerState {
            final CratesLocalIndexServiceImplInner myInner;
            Loaded(@NotNull CratesLocalIndexServiceImplInner inner) { myInner = inner; }
        }
        static class Err extends InnerState {
            final Error.InternalError myErr;
            Err(@NotNull Error.InternalError err) { myErr = err; }
        }
        static class Disposed extends InnerState {}
    }

    @FunctionalInterface
    private interface ThrowingFunction<T, R> {
        R apply(T t) throws IOException;
    }

    // Inner implementation
    static class CratesLocalIndexServiceImplInner {
        private final Path myCargoRegistryIndexPath;
        private final PersistentHashMap<String, CratesLocalIndexService.CargoRegistryCrate> myCrates;
        private final String myCargoRegistryIndexRefsLocation;
        private final LocalFileSystem.WatchRequest myWatchRequest;
        private final Path myIndexedCommitHashFile;
        private String myIndexedCommitHash;
        private final Object myIsClosedLock = new Object();
        private boolean myIsClosed = false;

        CratesLocalIndexServiceImplInner(@NotNull Path cargoRegistryIndexPath,
                                          @NotNull PersistentHashMap<String, CratesLocalIndexService.CargoRegistryCrate> crates,
                                          @NotNull String cargoRegistryIndexRefsLocation,
                                          @Nullable LocalFileSystem.WatchRequest watchRequest,
                                          @NotNull Path indexedCommitHashFile,
                                          @NotNull String indexedCommitHash) {
            myCargoRegistryIndexPath = cargoRegistryIndexPath;
            myCrates = crates;
            myCargoRegistryIndexRefsLocation = cargoRegistryIndexRefsLocation;
            myWatchRequest = watchRequest;
            myIndexedCommitHashFile = indexedCommitHashFile;
            myIndexedCommitHash = indexedCommitHash;
        }

        @NotNull
        String getCargoRegistryIndexRefsLocation() {
            return myCargoRegistryIndexRefsLocation;
        }

        @Nullable
        CratesLocalIndexService.CargoRegistryCrate getCrate(@NotNull String crateName) throws IOException {
            return myCrates.get(crateName);
        }

        @NotNull
        List<String> getAllCrateNames() throws IOException {
            List<String> crateNames = new ArrayList<>();
            myCrates.processKeys(name -> { crateNames.add(name); return true; });
            return crateNames;
        }

        void writeCratesUpdate(@NotNull List<Map.Entry<String, CratesLocalIndexService.CargoRegistryCrate>> updatedCrates,
                               @NotNull String newHeadHash) throws IOException {
            synchronized (myIsClosedLock) {
                if (myIsClosed) return;
                writeCommitHash(myIndexedCommitHashFile, INVALID_COMMIT_HASH);
            }

            for (Map.Entry<String, CratesLocalIndexService.CargoRegistryCrate> entry : updatedCrates) {
                myCrates.put(entry.getKey(), entry.getValue());
            }
            myCrates.force();

            synchronized (myIsClosedLock) {
                if (myIsClosed) return;
                writeCommitHash(myIndexedCommitHashFile, newHeadHash);
                myIndexedCommitHash = newHeadHash;
            }
        }

        @NotNull
        Task.Backgroundable createUpdateTask(
            @NotNull java.util.function.BiConsumer<CratesLocalIndexServiceImplInner, Error.InternalError> onError,
            @NotNull Runnable onFinish) {
            return new CratesLocalIndexUpdateTask(
                myCargoRegistryIndexPath,
                () -> myIndexedCommitHash,
                (updatedCrates, newHash) -> writeCratesUpdate(updatedCrates, newHash),
                err -> onError.accept(this, err),
                onFinish
            );
        }

        void close() {
            synchronized (myIsClosedLock) {
                myIsClosed = true;
            }
            if (myWatchRequest != null) {
                LocalFileSystem.getInstance().removeWatchedRoot(myWatchRequest);
            }
            try { myCrates.close(); } catch (IOException e) { LOG.warn(e); } catch (Throwable t) { LOG.error(t); }
        }

        @NotNull
        static RsResult<CratesLocalIndexServiceImplInner, Error.InternalError> tryCreate(
            @NotNull Path cargoRegistryIndexPath,
            @NotNull Path baseCratesLocalRegistryDir) {

            if (!OpenApiUtil.isUnitTestMode()) {
                OpenApiUtil.checkIsBackgroundThread();
            }

            Path cargoRegistryIndexRefsPath = cargoRegistryIndexPath.resolve("refs");
            if (!Files.exists(cargoRegistryIndexPath) || !Files.exists(cargoRegistryIndexRefsPath)) {
                return new RsResult.Err<>(new Error.InternalError.NoCargoIndex(cargoRegistryIndexPath));
            }

            String cargoRegistryIndexRefsLocation = cargoRegistryIndexRefsPath.toString();
            VirtualFile cargoRegistryIndexRefsVFile =
                LocalFileSystem.getInstance().refreshAndFindFileByPath(cargoRegistryIndexRefsLocation);

            if (cargoRegistryIndexRefsVFile == null) {
                LOG.error("Failed to subscribe to cargo registry changes in " + cargoRegistryIndexRefsLocation);
                return new RsResult.Err<>(new Error.InternalError.NoCargoIndex(cargoRegistryIndexRefsPath));
            }

            Path indexedCommitHashFile = baseCratesLocalRegistryDir.resolve("indexed-commit-hash");
            String indexedCommitHash = readCommitHash(indexedCommitHashFile);

            if (INVALID_COMMIT_HASH.equals(indexedCommitHash) && Files.exists(baseCratesLocalRegistryDir)) {
                try {
                    org.rust.stdext.PathUtil.cleanDirectory(baseCratesLocalRegistryDir);
                } catch (IOException e) {
                    LOG.error("Cannot clean directory " + baseCratesLocalRegistryDir, e);
                }
            }

            Path cratesFilePath = baseCratesLocalRegistryDir.resolve("crates-local-index");
            PersistentHashMap<String, CratesLocalIndexService.CargoRegistryCrate> crates;
            String[] indexedCommitHashHolder = {indexedCommitHash};
            try {
                crates = IOUtil.openCleanOrResetBroken(
                    () -> new PersistentHashMap<>(
                        cratesFilePath,
                        EnumeratorStringDescriptor.INSTANCE,
                        CrateExternalizer.INSTANCE,
                        4 * 1024,
                        CRATES_INDEX_VERSION
                    ),
                    () -> {
                        try {
                            org.rust.stdext.PathUtil.cleanDirectory(baseCratesLocalRegistryDir);
                        } catch (IOException ignored) {}
                        indexedCommitHashHolder[0] = INVALID_COMMIT_HASH;
                    }
                );
            } catch (IOException e) {
                LOG.error("Cannot open or create PersistentHashMap in " + cratesFilePath, e);
                return new RsResult.Err<>(new Error.InternalError.PersistentHashMapInitError(cratesFilePath, e.toString()));
            }

            LocalFileSystem.WatchRequest watchRequest =
                LocalFileSystem.getInstance().addRootToWatch(cargoRegistryIndexRefsLocation, true);

            VfsUtilCore.processFilesRecursively(cargoRegistryIndexRefsVFile, f -> true);
            RefreshQueue.getInstance().refresh(true, true, null, cargoRegistryIndexRefsVFile);

            CratesLocalIndexServiceImplInner inner = new CratesLocalIndexServiceImplInner(
                cargoRegistryIndexPath, crates, cargoRegistryIndexRefsLocation,
                watchRequest, indexedCommitHashFile, indexedCommitHashHolder[0]
            );
            return new RsResult.Ok<>(inner);
        }

        @NotNull
        private static String readCommitHash(@NotNull Path indexedCommitHashFile) {
            if (Files.exists(indexedCommitHashFile)) {
                try {
                    return Files.readString(indexedCommitHashFile);
                } catch (IOException e) {
                    LOG.warn("Cannot read file " + indexedCommitHashFile, e);
                    return INVALID_COMMIT_HASH;
                }
            }
            return INVALID_COMMIT_HASH;
        }

        private static void writeCommitHash(@NotNull Path file, @NotNull String hash) throws IOException {
            Files.writeString(file, hash);
        }
    }

    // CrateExternalizer
    static class CrateExternalizer implements DataExternalizer<CratesLocalIndexService.CargoRegistryCrate> {
        static final CrateExternalizer INSTANCE = new CrateExternalizer();

        @Override
        public void save(@NotNull DataOutput out, @NotNull CratesLocalIndexService.CargoRegistryCrate value) throws IOException {
            out.writeInt(value.getVersions().size());
            for (CratesLocalIndexService.CargoRegistryCrateVersion version : value.getVersions()) {
                out.writeUTF(version.getVersion());
                out.writeBoolean(version.isYanked());
                out.writeInt(version.getFeatures().size());
                for (String feature : version.getFeatures()) {
                    out.writeUTF(feature);
                }
            }
        }

        @NotNull
        @Override
        public CratesLocalIndexService.CargoRegistryCrate read(@NotNull DataInput inp) throws IOException {
            List<CratesLocalIndexService.CargoRegistryCrateVersion> versions = new ArrayList<>();
            int versionsSize = inp.readInt();
            for (int i = 0; i < versionsSize; i++) {
                String version = inp.readUTF();
                boolean yanked = inp.readBoolean();
                List<String> features = new ArrayList<>();
                int featuresSize = inp.readInt();
                for (int j = 0; j < featuresSize; j++) {
                    features.add(inp.readUTF());
                }
                versions.add(new CratesLocalIndexService.CargoRegistryCrateVersion(version, yanked, features));
            }
            return new CratesLocalIndexService.CargoRegistryCrate(versions);
        }
    }

    // ParsedVersion for JSON
    public static class ParsedVersion {
        public String name;
        public String vers;
        public boolean yanked;
        public HashMap<String, List<String>> features;
    }

    // Update task
    @FunctionalInterface
    interface CratesUpdateWriter {
        void write(List<Map.Entry<String, CratesLocalIndexService.CargoRegistryCrate>> updatedCrates, String newHeadHash) throws IOException;
    }

    private static class CratesLocalIndexUpdateTask extends Task.Backgroundable {
        private final Path myCargoRegistryIndexPath;
        private final java.util.function.Supplier<String> myIndexedCommitHashGetter;
        private final CratesUpdateWriter myWriteCratesUpdate;
        private final java.util.function.Consumer<Error.InternalError> myOnError;
        private final Runnable myOnFinish;

        CratesLocalIndexUpdateTask(@NotNull Path cargoRegistryIndexPath,
                                    @NotNull java.util.function.Supplier<String> indexedCommitHashGetter,
                                    @NotNull CratesUpdateWriter writeCratesUpdate,
                                    @NotNull java.util.function.Consumer<Error.InternalError> onError,
                                    @NotNull Runnable onFinish) {
            super(null, RsBundle.message("progress.title.loading.cargo.registry.index"), false);
            myCargoRegistryIndexPath = cargoRegistryIndexPath;
            myIndexedCommitHashGetter = indexedCommitHashGetter;
            myWriteCratesUpdate = writeCratesUpdate;
            myOnError = onError;
            myOnFinish = onFinish;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            FileRepositoryBuilder builder = new FileRepositoryBuilder()
                .setGitDir(myCargoRegistryIndexPath.toFile());

            List<Map.Entry<String, CratesLocalIndexService.CargoRegistryCrate>> updatedCrates;
            String newHeadHash;

            try {
                Repository repository = builder.build();
                try {
                    String indexedCommitHash = myIndexedCommitHashGetter.get();
                    newHeadHash = readRegistryHeadCommitHash(repository);
                    if (newHeadHash.equals(indexedCommitHash)) return;

                    indicator.checkCanceled();
                    updatedCrates = readNewCrates(indicator, repository, newHeadHash, indexedCommitHash);
                } finally {
                    RepositoryCache.unregister(repository);
                    repository.close();
                }
            } catch (RepositoryNotFoundException e) {
                LOG.warn(e);
                myOnError.accept(new Error.InternalError.NoCargoIndex(myCargoRegistryIndexPath));
                return;
            } catch (IOException e) {
                myOnError.accept(new Error.InternalError.RepoReadError(myCargoRegistryIndexPath, e.toString()));
                LOG.warn(e);
                return;
            }

            if (!updatedCrates.isEmpty()) {
                ProgressManager.getInstance().executeNonCancelableSection(() -> {
                    try {
                        myWriteCratesUpdate.write(updatedCrates, newHeadHash);
                    } catch (IOException e) {
                        LOG.warn(e);
                        myOnError.accept(new Error.InternalError.PersistentHashMapWriteError(e.toString()));
                    }
                });
            }
        }

        @Override
        public void onFinished() {
            myOnFinish.run();
        }

        @NotNull
        private String readRegistryHeadCommitHash(@NotNull Repository repository) throws IOException {
            ObjectId objectId = repository.resolve(CARGO_REGISTRY_INDEX_TAG);
            if (objectId != null) return objectId.name();
            LOG.error("Failed to resolve remote branch in the cargo registry index repository");
            return INVALID_COMMIT_HASH;
        }

        @NotNull
        private List<Map.Entry<String, CratesLocalIndexService.CargoRegistryCrate>> readNewCrates(
            @NotNull ProgressIndicator indicator,
            @NotNull Repository repository,
            @NotNull String newHeadHash,
            @NotNull String prevHeadHash) throws IOException {

            var reader = repository.newObjectReader();
            CanonicalTreeParser currentTreeIter = new CanonicalTreeParser();
            ObjectId currentHeadTree = resolveTreeObject(repository, newHeadHash);
            if (currentHeadTree == null) {
                LOG.error("Git revision `" + newHeadHash + "^{tree}` cannot be resolved to any object id");
                return Collections.emptyList();
            }
            currentTreeIter.reset(reader, currentHeadTree);

            TreeFilter filter;
            ObjectId prevHeadTree = resolveTreeObject(repository, prevHeadHash);
            if (prevHeadTree == null) {
                filter = TreeFilter.ALL;
            } else {
                CanonicalTreeParser prevTreeIter = new CanonicalTreeParser();
                prevTreeIter.reset(reader, prevHeadTree);

                Git git = new Git(repository);
                try {
                    List<DiffEntry> changes = git.diff()
                        .setNewTree(currentTreeIter)
                        .setOldTree(prevTreeIter)
                        .call();
                    if (changes.isEmpty()) {
                        filter = TreeFilter.ALL;
                    } else if (changes.size() == 1) {
                        filter = PathFilter.create(changes.get(0).getNewPath());
                    } else {
                        List<TreeFilter> filters = new ArrayList<>();
                        for (DiffEntry entry : changes) {
                            filters.add(PathFilter.create(entry.getNewPath()));
                        }
                        filter = OrTreeFilter.create(filters);
                    }
                } catch (GitAPIException e) {
                    LOG.error("Failed to calculate diff due to Git API error: " + e.getMessage());
                    filter = TreeFilter.ALL;
                }
            }

            var revTree = new RevWalk(repository).parseCommit(ObjectId.fromString(newHeadHash)).getTree();
            ObjectMapper mapper = JsonMapper.builder()
                .addModule(new KotlinModule.Builder().build())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

            List<Map.Entry<ObjectId, String>> objectIds = new ArrayList<>();
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(revTree);
                treeWalk.setFilter(filter);
                treeWalk.setRecursive(true);
                treeWalk.setPostOrderTraversal(false);

                while (treeWalk.next()) {
                    if (treeWalk.isSubtree()) continue;
                    String path = treeWalk.getPathString();
                    if (path.startsWith(".") || "config.json".equals(path)) continue;
                    indicator.checkCanceled();
                    ObjectId objectId = treeWalk.getObjectId(0);
                    objectIds.add(Map.entry(objectId, treeWalk.getNameString()));
                }
            }

            ExecutorService pool = Executors.newWorkStealingPool(2);
            try {
                return objectIds.parallelStream()
                    .map(entry -> {
                        indicator.checkCanceled();
                        try {
                            ObjectLoader loader = repository.open(entry.getKey());
                            List<CratesLocalIndexService.CargoRegistryCrateVersion> versions = new ArrayList<>();
                            BufferedReader fileReader = new BufferedReader(
                                new InputStreamReader(loader.openStream(), java.nio.charset.StandardCharsets.UTF_8));
                            String line;
                            while ((line = fileReader.readLine()) != null) {
                                if (line.isBlank()) continue;
                                try {
                                    versions.add(crateFromJson(line, mapper));
                                } catch (Exception e) {
                                    LOG.warn("Failed to parse JSON for crate " + entry.getValue() + ", line " + line, e);
                                }
                            }
                            return Map.entry(entry.getValue(), new CratesLocalIndexService.CargoRegistryCrate(versions));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .collect(Collectors.toList());
            } finally {
                pool.shutdownNow();
            }
        }

        @Nullable
        private ObjectId resolveTreeObject(@NotNull Repository repository, @NotNull String hash) throws IOException {
            try {
                return repository.resolve(hash + "^{tree}");
            } catch (MissingObjectException e) {
                LOG.warn(e);
                return null;
            }
        }

        @NotNull
        private static CratesLocalIndexService.CargoRegistryCrateVersion crateFromJson(@NotNull String json, @NotNull ObjectMapper mapper) throws IOException {
            ParsedVersion parsedVersion = mapper.readValue(json, ParsedVersion.class);
            List<String> features = parsedVersion.features != null
                ? new ArrayList<>(parsedVersion.features.keySet())
                : Collections.emptyList();
            return new CratesLocalIndexService.CargoRegistryCrateVersion(parsedVersion.vers, parsedVersion.yanked, features);
        }
    }
}
