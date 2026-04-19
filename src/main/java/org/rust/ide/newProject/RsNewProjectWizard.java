/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import com.intellij.ide.wizard.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.Panel;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.module.RsModuleBuilder;
import org.rust.stdext.PathUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RsNewProjectWizard implements LanguageNewProjectWizard {

    @NotNull
    @Override
    public String getName() {
        return "Rust"; // TODO: replace with `NewProjectWizardConstants.Language.RUST`
    }

    @Override
    public int getOrdinal() {
        return 900;
    }

    @NotNull
    @Override
    public NewProjectWizardStep createStep(@NotNull NewProjectWizardLanguageStep parent) {
        return new Step(parent);
    }

    private static class Step extends AbstractNewProjectWizardStep {
        private static final String GITIGNORE = ".gitignore";

        private final RsProjectGeneratorPeer peer;

        Step(@NotNull NewProjectWizardLanguageStep parent) {
            super(parent);
            Path path = PathUtil.toPathOrNull(parent.getPath());
            this.peer = new RsProjectGeneratorPeer(path != null ? path : Paths.get("."));
        }

        @Override
        public void setupUI(@NotNull Panel builder) {
            builder.row((String) null, row -> {
                row.cell(peer.getComponent())
                    .align(AlignX.FILL);
                return kotlin.Unit.INSTANCE;
            });
        }

        @Override
        public void setupProject(@NotNull Project project) {
            RsModuleBuilder builder = new RsModuleBuilder();
            List<Module> modules = builder.commit(project);
            if (modules == null || modules.isEmpty()) return;
            Module module = modules.get(0);
            ModuleRootModificationUtil.updateModel(module, rootModel -> {
                builder.setConfigurationData(peer.getSettings());
                builder.createProject(rootModel, "none");
                GitNewProjectWizardData gitData = GitNewProjectWizardData.getGitData(Step.this);
                if (gitData != null && gitData.getGit()) {
                    createGitIgnoreFile(getContext().getProjectDirectory(), module);
                }
            });
        }

        private static void createGitIgnoreFile(@NotNull Path projectDir, @NotNull Module module) {
            try {
                VirtualFile directory = VfsUtil.createDirectoryIfMissing(projectDir.toString());
                if (directory == null) return;
                VirtualFile existingFile = directory.findChild(GITIGNORE);
                if (existingFile != null) return;
                VirtualFile file = directory.createChildData(module, GITIGNORE);
                VfsUtil.saveText(file, "/target\n");
            } catch (Exception ignored) {
            }
        }
    }
}
