/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

@Service
public final class DefMapService implements Disposable {
    @NotNull
    private final Project project;

    @NotNull
    private final ConcurrentMap<Integer, DefMapHolder> defMaps = new ConcurrentHashMap<>();
    @NotNull
    private final ReentrantLock defMapsBuildLock = new ReentrantLock();

    @NotNull
    private final MultiMap<Integer, Integer> fileIdToCrateId = MultiMap.createConcurrent();

    @NotNull
    private final ConcurrentHashMap<Path, Integer> missedFiles = new ConcurrentHashMap<>();

    @NotNull
    private final AtomicReference<WeakReference<List<Crate>>> lastCheckedTopSortedCrates = new AtomicReference<>(null);

    @NotNull
    private final ModificationTracker structureModificationTracker;

    private volatile long allDefMapsUpdatedStamp = -1;

    private static final AtomicInteger nextNonCargoCrateId = new AtomicInteger(-1);

    public DefMapService(@NotNull Project project) {
        this.project = project;
        this.structureModificationTracker =
            project.getService(RsPsiManager.class).getRustStructureModificationTracker();
        setupListeners();
    }

    private void setupListeners() {
        PsiManager.getInstance(project).addPsiTreeChangeListener(new DefMapPsiTreeChangeListener(), this);

        project.getMessageBus().connect().subscribe(
            CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (CargoProjectsService.CargoProjectsListener) (oldProjects, newProjects) -> scheduleRecheckAllDefMaps()
        );
    }

    @NotNull
    public Project getProject() {
        return project;
    }

    @NotNull
    public ReentrantLock getDefMapsBuildLock() {
        return defMapsBuildLock;
    }

    @NotNull
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

    private void onFileAdded(@NotNull RsFile file) {
        OpenApiUtil.checkWriteAccessAllowed();
        Path path = OpenApiUtil.getPathAsPath(file.getVirtualFile());
        Integer crateId = missedFiles.get(path);
        if (crateId == null) return;
        getDefMapHolder(crateId).setShouldRebuild(true);
    }

    private void onFileRemoved(@NotNull RsFile file) {
        OpenApiUtil.checkWriteAccessAllowed();
        for (int crateId : findCrates(file)) {
            getDefMapHolder(crateId).setShouldRebuild(true);
        }
    }

    public void onFileChanged(@NotNull RsFile file) {
        OpenApiUtil.checkWriteAccessAllowed();
        for (int crateId : findCrates(file)) {
            getDefMapHolder(crateId).addChangedFile(file);
        }
    }

    @NotNull
    public Collection<Integer> findCrates(@NotNull RsFile file) {
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

    @NotNull
    public List<CrateDefMap> updateDefMapForAllCratesWithWriteActionPriority(@NotNull com.intellij.openapi.progress.ProgressIndicator indicator) {
        return FacadeUpdateDefMap.updateDefMapForAllCratesWithWriteActionPriority(this, indicator, true);
    }

    public void removeStaleDefMaps(@NotNull List<Crate> allCrates) {
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
        public void handleEvent(@NotNull RsPsiTreeChangeEvent event) {
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
