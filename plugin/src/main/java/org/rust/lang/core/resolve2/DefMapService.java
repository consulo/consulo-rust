/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.application.progress.ProgressIndicator;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.component.util.ModificationTracker;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.util.collection.MultiMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CratePersistentId;
import org.rust.lang.core.macros.MacroExpansionMode;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsPsiTreeChangeEvent.*;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.CollectionsUtil;

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;
import jakarta.inject.Inject;
import org.rust.cargo.project.model.CargoProjectsListener;
import org.rust.lang.core.psi.RsPsiTreeChangeEvent;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class DefMapService implements Disposable {
    @Nonnull
    private final Project project;

    @Nonnull
    private final ConcurrentMap<Integer, DefMapHolder> defMaps = new ConcurrentHashMap<>();
    @Nonnull
    private final ReentrantLock defMapsBuildLock = new ReentrantLock();

    @Nonnull
    private final MultiMap<Integer, Integer> fileIdToCrateId = MultiMap.createConcurrent();

    @Nonnull
    private final ConcurrentHashMap<Path, Integer> missedFiles = new ConcurrentHashMap<>();

    @Nonnull
    private final AtomicReference<WeakReference<List<Crate>>> lastCheckedTopSortedCrates = new AtomicReference<>(null);

    @Nonnull
    private final ModificationTracker structureModificationTracker;

    private volatile long allDefMapsUpdatedStamp = -1;

    private static final AtomicInteger nextNonCargoCrateId = new AtomicInteger(-1);

    @Inject

    public DefMapService(@Nonnull Project project) {
        this.project = project;
        this.structureModificationTracker =
            project.getService(RsPsiManager.class).getRustStructureModificationTracker();
        setupListeners();
    }

    private void setupListeners() {
        PsiManager.getInstance(project).addPsiTreeChangeListener(new DefMapPsiTreeChangeListener(), this);

        project.getMessageBus().connect().subscribe(
            CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (CargoProjectsListener) (oldProjects, newProjects) -> scheduleRecheckAllDefMaps()
        );
    }

    @Nonnull
    public Project getProject() {
        return project;
    }

    @Nonnull
    public ReentrantLock getDefMapsBuildLock() {
        return defMapsBuildLock;
    }

    @Nonnull
    public DefMapHolder getDefMapHolder(int crateId) {
        return defMaps.computeIfAbsent(crateId, id -> new DefMapHolder(id, structureModificationTracker));
    }

    public boolean hasDefMapFor(int crateId) {
        return defMaps.get(crateId) != null;
    }

    public void setDefMap(int crateId, @Nullable CrateDefMap defMap) {
        updateFilesMaps(crateId, defMap);
        DefMapHolder holder = getDefMapHolder(crateId);
        holder.setDefMap(defMap);
    }

    private void updateFilesMaps(int crateId, @Nullable CrateDefMap defMap) {
        fileIdToCrateId.values().removeIf(it -> it == crateId);
        missedFiles.values().removeIf(it -> it == crateId);
        if (defMap != null) {
            for (int fileId : defMap.getFileInfos().keySet()) {
                fileIdToCrateId.putValue(fileId, crateId);
            }
            for (Path missedFile : defMap.getMissedFiles()) {
                missedFiles.put(missedFile, crateId);
            }
        }
    }

    private void onFileAdded(@Nonnull RsFile file) {
        OpenApiUtil.checkWriteAccessAllowed();
        Path path = OpenApiUtil.getPathAsPath(file.getVirtualFile());
        Integer crateId = missedFiles.get(path);
        if (crateId == null) return;
        getDefMapHolder(crateId).setShouldRebuild(true);
    }

    private void onFileRemoved(@Nonnull RsFile file) {
        OpenApiUtil.checkWriteAccessAllowed();
        for (int crateId : findCrates(file)) {
            getDefMapHolder(crateId).setShouldRebuild(true);
        }
    }

    public void onFileChanged(@Nonnull RsFile file) {
        OpenApiUtil.checkWriteAccessAllowed();
        for (int crateId : findCrates(file)) {
            getDefMapHolder(crateId).addChangedFile(file);
        }
    }

    @Nonnull
    public Collection<Integer> findCrates(@Nonnull RsFile file) {
        if (!(file.getVirtualFile() instanceof VirtualFileWithId)) return Collections.emptyList();
        return fileIdToCrateId.get(((VirtualFileWithId) file.getVirtualFile()).getId());
    }

    public void scheduleRebuildAllDefMaps() {
        for (DefMapHolder holder : defMaps.values()) {
            holder.setShouldRebuild(true);
        }
    }

    public void scheduleRebuildDefMap(int crateId) {
        getDefMapHolder(crateId).setShouldRebuild(true);
    }

    private void scheduleRecheckAllDefMaps() {
        OpenApiUtil.checkWriteAccessAllowed();
        for (DefMapHolder holder : defMaps.values()) {
            holder.setShouldRecheck(true);
        }
    }

    @Nonnull
    public List<CrateDefMap> updateDefMapForAllCratesWithWriteActionPriority(@Nonnull consulo.application.progress.ProgressIndicator indicator) {
        return FacadeUpdateDefMap.updateDefMapForAllCratesWithWriteActionPriority(this, indicator, true);
    }

    public void removeStaleDefMaps(@Nonnull List<Crate> allCrates) {
        WeakReference<List<Crate>> prev = lastCheckedTopSortedCrates.getAndSet(new WeakReference<>(allCrates));
        if (prev != null && prev.get() == allCrates) return;

        Set<Integer> allCrateIds = new HashSet<>();
        for (Crate c : allCrates) {
            Integer id = c.getId();
            if (id != null) allCrateIds.add(id);
        }
        Set<Integer> staleCrates = new HashSet<>();
        defMaps.keySet().removeIf(crateId -> {
            boolean isStale = !allCrateIds.contains(crateId);
            if (isStale) staleCrates.add(crateId);
            return isStale;
        });
        fileIdToCrateId.values().removeIf(staleCrates::contains);
        missedFiles.values().removeIf(staleCrates::contains);
    }

    public void setAllDefMapsUpToDate() {
        allDefMapsUpdatedStamp = structureModificationTracker.getModificationCount();
    }

    public boolean areAllDefMapsUpToDate() {
        return allDefMapsUpdatedStamp == structureModificationTracker.getModificationCount();
    }

    @Override
    public void dispose() {}

    public static int getNextNonCargoCrateId() {
        return nextNonCargoCrateId.decrementAndGet();
    }

    private class DefMapPsiTreeChangeListener extends RsPsiTreeChangeAdapter {
        @Override
        public void handleEvent(@Nonnull RsPsiTreeChangeEvent event) {
            if (event.getFile() != null) return;
            if (event instanceof ChildAddition.After) {
                PsiElement child = ((ChildAddition.After) event).getChild();
                if (child instanceof RsFile) {
                    onFileAdded((RsFile) child);
                }
            } else if (event instanceof ChildRemoval.Before) {
                PsiElement child = ((ChildRemoval.Before) event).getChild();
                if (child instanceof RsFile) {
                    onFileRemoved((RsFile) child);
                }
            } else if (event instanceof PropertyChange.Before) {
                PropertyChange.Before propEvent = (PropertyChange.Before) event;
                if (PsiTreeChangeEvent.PROP_FILE_NAME.equals(propEvent.getPropertyName())) {
                    PsiElement child = propEvent.getChild();
                    if (child instanceof RsFile) {
                        onFileRemoved((RsFile) child);
                    }
                }
            } else if (event instanceof PropertyChange.After) {
                PropertyChange.After propEvent = (PropertyChange.After) event;
                if (PsiTreeChangeEvent.PROP_FILE_NAME.equals(propEvent.getPropertyName())) {
                    PsiElement element = propEvent.getElement();
                    if (element instanceof RsFile) {
                        onFileAdded((RsFile) element);
                    }
                }
            }
        }
    }
}
