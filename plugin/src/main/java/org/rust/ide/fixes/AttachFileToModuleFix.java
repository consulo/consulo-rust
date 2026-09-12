/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.project.ui.notification.NotificationType;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.awt.SimpleListCellRenderer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectsUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.ide.notifications.NotificationUtils;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsItemsOwnerUtil;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.VirtualFileExtUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.rust.lang.core.psi.ext.RsFileUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.DialogBuilder;

/**
 * Attaches a file to a Rust module.
 */
public class AttachFileToModuleFix extends LocalQuickFixOnPsiElement {
    private final String myTargetModuleName;

    public AttachFileToModuleFix(@Nonnull RsFile file) {
        this(file, null);
    }

    public AttachFileToModuleFix(@Nonnull RsFile file, @Nullable String targetModuleName) {
        super(file);
        this.myTargetModuleName = targetModuleName;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.attach.file.to", myTargetModuleName != null ? myTargetModuleName : "a module"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
        if (!(startElement instanceof RsFile)) return;
        RsFile rsFile = (RsFile) startElement;
        List<RsFile> availableModules = findAvailableModulesForFile(project, rsFile);

        switch (availableModules.size()) {
            case 0:
                return;
            case 1:
                insertFileToModule(rsFile, availableModules.get(0));
                break;
            default:
                RsFile selected = selectModule(rsFile, availableModules);
                if (selected != null) {
                    insertFileToModule(rsFile, selected);
                }
                break;
        }
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    private void insertFileToModule(@Nonnull RsFile file, @Nonnull RsFile targetFile) {
        Project project = file.getProject();
        RsPsiFactory factory = new RsPsiFactory(project);

        // if the filename is mod.rs, attach its parent directory
        String name;
        if (isModuleFile(file)) {
            name = file.getVirtualFile().getParent().getName();
        } else {
            name = file.getVirtualFile().getNameWithoutExtension();
        }

        RsModDeclItem modItem = factory.tryCreateModDeclItem(name);
        if (modItem == null) {
            NotificationUtils.showBalloon(project, RsBundle.message("notification.content.could.not.create.mod", name), NotificationType.ERROR);
            return;
        }

        OpenApiUtil.runWriteCommandAction(project, getText().get(), new consulo.language.psi.PsiFile[]{targetFile}, () -> {
            insertModItem(modItem, targetFile).navigate(true);
            return null;
        });
    }

    @Nullable
    public static AttachFileToModuleFix createIfCompatible(@Nonnull Project project, @Nonnull RsFile file) {
        List<RsFile> availableModules = findAvailableModulesForFile(project, file);
        if (!availableModules.isEmpty()) {
            String moduleLabel = availableModules.size() == 1 ? availableModules.get(0).getName() : null;
            return new AttachFileToModuleFix(file, moduleLabel);
        } else {
            return null;
        }
    }

    @Nonnull
    private static List<RsFile> findAvailableModulesForFile(@Nonnull Project project, @Nonnull RsFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) return new ArrayList<>();
        consulo.language.psi.PsiFile rawPkg = null;
        // Simplified: find package for file
        Object pkg = CargoProjectsUtil.getCargoProjects(project).findPackageForFile(virtualFile);
        if (pkg == null) return new ArrayList<>();
        VirtualFile directory = virtualFile.getParent();
        if (directory == null) return new ArrayList<>();

        List<RsFile> modules = new ArrayList<>();

        if (isModuleFile(file)) {
            RsFile modFile = findModule(file, project, directory.getParent() != null ? directory.getParent().findFileByRelativePath(RsConstants.MOD_RS_FILE) : null);
            if (modFile != null) modules.add(modFile);

            for (Object target : ((org.rust.cargo.project.workspace.CargoWorkspace.Package) pkg).getTargets()) {
                VirtualFile crateRoot = ((org.rust.cargo.project.workspace.CargoWorkspace.Target) target).getCrateRoot();
                if (crateRoot == null) continue;
                if (crateRoot.getParent() == directory.getParent()) {
                    PsiFile psiFile = VirtualFileExtUtil.toPsiFile(crateRoot, project);
                    RsFile rsFile = psiFile != null ? RsFileUtil.getRustFile(psiFile) : null;
                    if (rsFile != null) modules.add(rsFile);
                }
            }
        } else {
            RsFile modFile = findModule(file, project, directory.findFileByRelativePath(RsConstants.MOD_RS_FILE));
            if (modFile != null) modules.add(modFile);

            if (((org.rust.cargo.project.workspace.CargoWorkspace.Package) pkg).getEdition().compareTo(CargoWorkspace.Edition.EDITION_2018) >= 0) {
                RsFile parentModFile = findModule(file, project,
                    directory.getParent() != null ? directory.getParent().findFileByRelativePath(directory.getName() + ".rs") : null);
                if (parentModFile != null) modules.add(parentModFile);
            }

            for (Object target : ((org.rust.cargo.project.workspace.CargoWorkspace.Package) pkg).getTargets()) {
                VirtualFile crateRoot = ((org.rust.cargo.project.workspace.CargoWorkspace.Target) target).getCrateRoot();
                if (crateRoot == null) continue;
                if (crateRoot.getParent() == directory) {
                    PsiFile psiFile = VirtualFileExtUtil.toPsiFile(crateRoot, project);
                    RsFile rsFile = psiFile != null ? RsFileUtil.getRustFile(psiFile) : null;
                    if (rsFile != null) modules.add(rsFile);
                }
            }
        }

        return modules;
    }

    @Nullable
    private static RsFile selectModule(@Nonnull RsFile file, @Nonnull List<RsFile> availableModules) {
        if (OpenApiUtil.isUnitTestMode()) {
            BiFunction<RsFile, List<RsFile>, RsFile> mock = MOCK;
            if (mock == null) throw new IllegalStateException("You should set mock module selector via withMockModuleAttachSelector");
            return mock.apply(file, availableModules);
        }

        ComboBox<RsFile> box = new ComboBox<>();
        for (RsFile module : availableModules) {
            box.addItem(module);
        }
        box.setRenderer(SimpleListCellRenderer.create("", it -> {
            Path root = RsElementUtil.getContainingCargoPackage(it) != null
                ? RsElementUtil.getContainingCargoPackage(it).getRootDirectory() : null;
            Path path = it.getContainingFile().getVirtualFile().toNioPath();
            return (root != null ? root.relativize(path) : path).toString();
        }));

        // Simplified dialog creation
        consulo.ui.ex.awt.DialogBuilder builder = new consulo.ui.ex.awt.DialogBuilder(file.getProject());
        builder.setCenterPanel(box);
        builder.setTitle(RsBundle.message("dialog.title.select.module"));
        if (!builder.showAndGet()) return null;
        return (RsFile) box.getSelectedItem();
    }

    @Nullable
    private static RsFile findModule(@Nonnull RsFile root, @Nonnull Project project, @Nullable VirtualFile file) {
        if (file == null) return null;
        PsiFile psiFile = VirtualFileExtUtil.toPsiFile(file, project);
        RsFile module = psiFile != null ? RsFileUtil.getRustFile(psiFile) : null;
        if (module == null || module == root || module.getCrateRoot() == null) return null;
        return module;
    }

    @Nonnull
    private static RsModDeclItem insertModItem(@Nonnull RsModDeclItem item, @Nonnull RsFile module) {
        PsiElement anchor = RsItemsOwnerUtil.getFirstItem(module);
        List<RsModDeclItem> existingMods = RsElementUtil.childrenOfType(module, RsModDeclItem.class);
        RsModDeclItem existingMod = existingMods.isEmpty() ? null : existingMods.get(existingMods.size() - 1);

        PsiElement inserted;
        if (existingMod != null) {
            inserted = module.addAfter(item, existingMod);
        } else if (anchor != null) {
            inserted = module.addBefore(item, anchor);
        } else {
            inserted = module.add(item);
        }
        return (RsModDeclItem) inserted;
    }

    private static boolean isModuleFile(@Nonnull RsFile file) {
        return RsConstants.MOD_RS_FILE.equals(file.getName());
    }

    private static volatile BiFunction<RsFile, List<RsFile>, RsFile> MOCK = null;

    
    public static void withMockModuleAttachSelector(
        @Nonnull BiFunction<RsFile, List<RsFile>, RsFile> mock,
        @Nonnull Runnable f
    ) {
        MOCK = mock;
        try {
            f.run();
        } finally {
            MOCK = null;
        }
    }
}
