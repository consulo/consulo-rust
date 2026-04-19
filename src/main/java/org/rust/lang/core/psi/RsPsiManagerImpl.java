/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.ProjectTopics;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.RsFileType;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.macros.MacroExpansionFileSystem;
import org.rust.lang.core.macros.MacroExpansionManagerUtil;
import org.rust.lang.core.macros.MacroExpansionMode;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.resolve2.DefMapService;

import java.util.List;

public class RsPsiManagerImpl implements RsPsiManager, Disposable {
    @NotNull
    private final Project myProject;
    @NotNull
    private final SimpleModificationTracker myRustStructureModificationTracker = new SimpleModificationTracker();
    @NotNull
    private final SimpleModificationTracker myRustStructureModificationTrackerInDependencies = new SimpleModificationTracker();

    public RsPsiManagerImpl(@NotNull Project project) {
        myProject = project;
        PsiManager.getInstance(project).addPsiTreeChangeListener(new CacheInvalidator(), this);
        project.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
            @Override
            public void rootsChanged(@NotNull ModuleRootEvent event) {
                incRustStructureModificationCount();
            }
        });
        project.getMessageBus().connect().subscribe(CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (CargoProjectsService.CargoProjectsListener) (prev, cur) -> incRustStructureModificationCount());
    }

    @Override
    public void dispose() {
    }

    @NotNull
    @Override
    public ModificationTracker getRustStructureModificationTracker() {
        return myRustStructureModificationTracker;
    }

    @NotNull
    @Override
    public SimpleModificationTracker getRustStructureModificationTrackerInDependencies() {
        return myRustStructureModificationTrackerInDependencies;
    }

    @Override
    public void incRustStructureModificationCount() {
        incRustStructureModificationCount(null, null);
    }

    private void incRustStructureModificationCount(@Nullable PsiFile file, @Nullable PsiElement psi) {
        myRustStructureModificationTracker.incModificationCount();
        if (!isWorkspaceFile(file)) {
            myRustStructureModificationTrackerInDependencies.incModificationCount();
        }
        myProject.getMessageBus().syncPublisher(RsPsiManagerUtil.getRUST_STRUCTURE_CHANGE_TOPIC())
            .rustStructureChanged(file, psi);
    }

    private boolean isWorkspaceFile(@Nullable PsiFile file) {
        if (!(file instanceof RsFile)) return false;
        com.intellij.openapi.vfs.VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) return false;
        List<Object> crates;
        if (virtualFile.getFileSystem() instanceof MacroExpansionFileSystem) {
            Object crateId = MacroExpansionManagerUtil.getMacroExpansionManagerIfCreated(myProject) != null
                ? MacroExpansionManagerUtil.getMacroExpansionManagerIfCreated(myProject).getCrateForExpansionFile(virtualFile)
                : null;
            if (crateId == null) return false;
            crates = java.util.Collections.singletonList(crateId);
        } else {
            crates = (List<Object>) (List<?>) myProject.getService(DefMapService.class).findCrates((RsFile) file);
        }
        if (crates.isEmpty()) return false;
        org.rust.lang.core.crate.CrateGraphService crateGraph = CrateGraphService.crateGraph(myProject);
        for (Object crateId : crates) {
            org.rust.lang.core.crate.Crate crate = crateGraph.findCrateById(((Number) crateId).intValue());
            if (crate != null && crate.getOrigin() != PackageOrigin.WORKSPACE) return false;
        }
        return true;
    }

    private boolean isMacroExpansionModeNew() {
        Object mgr = MacroExpansionManagerUtil.getMacroExpansionManagerIfCreated(myProject);
        if (mgr == null) return false;
        try {
            java.lang.reflect.Method method = mgr.getClass().getMethod("getMacroExpansionMode");
            Object mode = method.invoke(mgr);
            return mode instanceof MacroExpansionMode.New;
        } catch (Exception e) {
            return false;
        }
    }

    private class CacheInvalidator extends RsPsiTreeChangeAdapter {
        @Override
        public void handleEvent(@NotNull RsPsiTreeChangeEvent event) {
            PsiElement element;
            if (event instanceof RsPsiTreeChangeEvent.ChildRemoval.Before) {
                element = ((RsPsiTreeChangeEvent.ChildRemoval.Before) event).getChild();
            } else if (event instanceof RsPsiTreeChangeEvent.ChildRemoval.After) {
                element = ((RsPsiTreeChangeEvent.ChildRemoval.After) event).getParent();
            } else if (event instanceof RsPsiTreeChangeEvent.ChildReplacement.Before) {
                element = ((RsPsiTreeChangeEvent.ChildReplacement.Before) event).getOldChild();
            } else if (event instanceof RsPsiTreeChangeEvent.ChildReplacement.After) {
                element = ((RsPsiTreeChangeEvent.ChildReplacement.After) event).getNewChild();
            } else if (event instanceof RsPsiTreeChangeEvent.ChildAddition.After) {
                element = ((RsPsiTreeChangeEvent.ChildAddition.After) event).getChild();
            } else if (event instanceof RsPsiTreeChangeEvent.ChildMovement.After) {
                element = ((RsPsiTreeChangeEvent.ChildMovement.After) event).getChild();
            } else if (event instanceof RsPsiTreeChangeEvent.ChildrenChange.After) {
                if (((RsPsiTreeChangeEvent.ChildrenChange.After) event).isGenericChange()) return;
                element = ((RsPsiTreeChangeEvent.ChildrenChange.After) event).getParent();
            } else if (event instanceof RsPsiTreeChangeEvent.PropertyChange.After) {
                RsPsiTreeChangeEvent.PropertyChange.After propChange =
                    (RsPsiTreeChangeEvent.PropertyChange.After) event;
                String propName = propChange.getPropertyName();
                if (PsiTreeChangeEvent.PROP_UNLOADED_PSI.equals(propName) ||
                    PsiTreeChangeEvent.PROP_FILE_TYPES.equals(propName)) {
                    incRustStructureModificationCount();
                    return;
                }
                if (PsiTreeChangeEvent.PROP_WRITABLE.equals(propName)) return;
                element = propChange.getElement();
                if (element == null) return;
            } else {
                return;
            }

            PsiFile file = event.getFile();
            if (file == null) {
                boolean isStructureModification =
                    (element instanceof RsFile && !RsPsiManager.isIgnorePsiEvents((RsFile) element)) ||
                    (element instanceof PsiDirectory &&
                        CargoProjectServiceUtil.getCargoProjects(myProject).findPackageForFile(
                            ((PsiDirectory) element).getVirtualFile()) != null);
                if (isStructureModification) {
                    incRustStructureModificationCount(
                        element instanceof RsFile ? (RsFile) element : null,
                        element instanceof RsFile ? (RsFile) element : null
                    );
                }
            } else {
                if (file.getFileType() != RsFileType.INSTANCE) return;
                if (RsPsiManager.isIgnorePsiEvents(file)) return;

                boolean isWhitespaceOrComment = element instanceof PsiComment || element instanceof PsiWhiteSpace;
                if (isWhitespaceOrComment && !isMacroExpansionModeNew()) return;

                boolean isChildrenChange = event instanceof RsPsiTreeChangeEvent.ChildrenChange ||
                    event instanceof RsPsiTreeChangeEvent.ChildRemoval.After;

                updateModificationCount(file, element, isChildrenChange, isWhitespaceOrComment);
            }
        }
    }

    private void updateModificationCount(
        @NotNull PsiFile file,
        @NotNull PsiElement psi,
        boolean isChildrenChange,
        boolean isWhitespaceOrComment
    ) {
        RsModificationTrackerOwner owner = DumbService.isDumb(myProject)
            ? null
            : RsModificationTrackerOwnerUtil.findModificationTrackerOwner(psi, !isChildrenChange);

        if (isWhitespaceOrComment) {
            if (!(owner instanceof RsMacroCall) && !(owner instanceof RsMacroDefinitionBase) &&
                !RsProcMacroPsiUtil.canBeInProcMacroCallBody(psi)) return;
        }

        boolean isStructureModification = owner == null || !owner.incModificationCount(psi);

        if (!isStructureModification && owner instanceof RsMacroCall) {
            if (!isMacroExpansionModeNew() || !RsMacroCallUtil.isTopLevelExpansion((RsMacroCall) owner)) {
                updateModificationCount(file, (PsiElement) owner, false, false);
                return;
            }
        }

        if (isStructureModification) {
            incRustStructureModificationCount(file, psi);
        }
        myProject.getMessageBus().syncPublisher(RsPsiManagerUtil.getRUST_PSI_CHANGE_TOPIC())
            .rustPsiChanged(file, psi, isStructureModification);
    }
}
