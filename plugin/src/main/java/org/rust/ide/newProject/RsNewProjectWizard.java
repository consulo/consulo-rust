/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import com.intellij.ide.wizard.*;
import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.util.ModuleRootModificationUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.ui.dsl.builder.AlignX;
import com.intellij.ui.dsl.builder.Panel;
import jakarta.annotation.Nonnull;
import org.rust.ide.module.RsModuleBuilder;
import org.rust.stdext.PathUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class RsNewProjectWizard implements LanguageNewProjectWizard {

    @Nonnull
    public String getName() {
        return "Rust"; // TODO: replace with `NewProjectWizardConstants.Language.RUST`
    }

    public int getOrdinal() {
        return 900;
    }

    @Nonnull
    public NewProjectWizardStep createStep(@Nonnull NewProjectWizardLanguageStep parent) {
        return new Step(parent);
    }

    private static class Step extends AbstractNewProjectWizardStep implements NewProjectWizardStep {
        private static final String GITIGNORE = ".gitignore";

        private final RsProjectGeneratorPeer peer;

        Step(@Nonnull NewProjectWizardLanguageStep parent) {
            super(parent);
            Path path = PathUtil.toPathOrNull(parent.getPath());
            this.peer = new RsProjectGeneratorPeer(path != null ? path : Paths.get("."));
        }

        public void setupUI(@Nonnull Panel builder) {
            builder.row((String) null, row -> {
                row.cell(peer.getComponent())
                    .align(AlignX.FILL);
                return null;
            });
        }

        public void setupProject(@Nonnull Project project) {
            // TODO: wire RsModuleBuilder.commit + ModuleRootModificationUtil to Consulo's new wizard APIs
            new RsModuleBuilder();
        }

        private static void createGitIgnoreFile(@Nonnull Path projectDir, @Nonnull Module module) {
            try {
                VirtualFile directory = VirtualFileUtil.createDirectoryIfMissing(projectDir.toString());
                if (directory == null) return;
                VirtualFile existingFile = directory.findChild(GITIGNORE);
                if (existingFile != null) return;
                VirtualFile file = directory.createChildData(module, GITIGNORE);
                VirtualFileUtil.saveText(file, "/target\n");
            } catch (Exception ignored) {
            }
        }
    }
}
