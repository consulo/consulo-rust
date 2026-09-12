/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.disposer.Disposable;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.component.util.ModificationTracker;
import consulo.component.util.SimpleModificationTracker;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.psi.event.PsiTreeChangeEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.RsFileType;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.macros.MacroExpansionFileSystem;
import org.rust.lang.core.macros.MacroExpansionManager;
import org.rust.lang.core.macros.MacroExpansionManagerUtil;
import org.rust.lang.core.macros.MacroExpansionMode;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.resolve2.DefMapService;

import java.util.List;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Inject;
import org.rust.cargo.project.model.CargoProjectsListener;
import consulo.virtualFileSystem.VirtualFile;
import org.rust.lang.core.crate.Crate;

@ServiceImpl
public class RsPsiManagerImpl implements RsPsiManager, Disposable {
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final SimpleModificationTracker myRustStructureModificationTracker = new SimpleModificationTracker();
    @Nonnull
    private final SimpleModificationTracker myRustStructureModificationTrackerInDependencies = new SimpleModificationTracker();

    @Inject

    public RsPsiManagerImpl(@Nonnull Project project) {
        myProject = project;
        PsiManager.getInstance(project).addPsiTreeChangeListener(new CacheInvalidator(), this);
        project.getMessageBus().connect().subscribe(ModuleRootListener.class, new ModuleRootListener() {
            @Override
            public void rootsChanged(@Nonnull ModuleRootEvent event) {
                incRustStructureModificationCount();
            }
        });
        project.getMessageBus().connect().subscribe(CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (CargoProjectsListener) (prev, cur) -> incRustStructureModificationCount());
    }

    @Override
    public void dispose() {
    }

    @Nonnull
    @Override
    public ModificationTracker getRustStructureModificationTracker() {
        return myRustStructureModificationTracker;
    }

    @Nonnull
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
        consulo.virtualFileSystem.VirtualFile virtualFile = file.getVirtualFile();
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
            // A crate id the graph does not know is not a workspace crate either.
            if (crate == null || crate.getOrigin() != PackageOrigin.WORKSPACE) return false;
        }
        return true;
    }

    private boolean isMacroExpansionModeNew() {
        MacroExpansionManager manager = MacroExpansionManagerUtil.getMacroExpansionManagerIfCreated(myProject);
        return manager != null && manager.getMacroExpansionMode() instanceof MacroExpansionMode.New;
    }

    private class CacheInvalidator extends RsPsiTreeChangeAdapter {
        @Override
        public void handleEvent(@Nonnull RsPsiTreeChangeEvent event) {
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
        @Nonnull PsiFile file,
        @Nonnull PsiElement psi,
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
