/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.rustfmt;

import org.rust.cargo.toolchain.RsToolchainLocator;

import consulo.application.ApplicationManager;
import jakarta.annotation.Nullable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.virtualFileSystem.VirtualFile;
import consulo.document.util.DocumentUtil;
import consulo.util.collection.ContainerUtil;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.ide.rustfmt.Rustfmt;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;
import consulo.application.WriteAction;

@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public final class RustfmtWatcher {
    final Set<Document> documentsToReformatLater = ContainerUtil.newConcurrentSet();
    private boolean isSuppressed = false;

    public void withoutReformatting(Runnable action) {
        boolean oldStatus = isSuppressed;
        try {
            isSuppressed = true;
            action.run();
        } finally {
            isSuppressed = oldStatus;
        }
    }

    public boolean reformatDocumentLater(Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) return false;
        if (RsFile.isNotRustFile(file)) return false;
        Project project = ProjectLocator.getInstance().guessProjectForFile(file);
        if (project == null) return false;
        if (!RsProjectSettingsServiceUtil.getRustfmtSettings(project).getRunRustfmtOnSave()) return false;
        return documentsToReformatLater.add(document);
    }

    public static RustfmtWatcher getInstance() {
        return ApplicationManager.getApplication().getService(RustfmtWatcher.class);
    }

    /** Returns the watcher only when it is already loaded, never creating it. */
    @Nullable
    static RustfmtWatcher getInstanceIfCreated() {
        return ApplicationManager.getApplication().getInstanceIfCreated(RustfmtWatcher.class);
    }

    static CargoProject findCargoProject(Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) return null;
        Project project = ProjectLocator.getInstance().guessProjectForFile(file);
        if (project == null) return null;
        return CargoProjectServiceUtil.getCargoProjects(project).findProjectForFile(file);
    }

    static void reformatDocuments(CargoProject cargoProject, List<Document> documents) {
        Project project = cargoProject.getProject();
        if (!RsProjectSettingsServiceUtil.getRustfmtSettings(project).getRunRustfmtOnSave()) return;
        RsToolchainBase toolchain = RsToolchainLocator.fromSettings(project);
        if (toolchain == null) return;
        Rustfmt rustfmt = Rustfmt.create(toolchain);
        if (rustfmt == null) return;
        if (Rustup.checkNeedInstallRustfmt(cargoProject.getProject(), org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(cargoProject))) return;
        for (Document document : documents) {
            reformatDocument(rustfmt, cargoProject, document);
        }
    }

    private static void reformatDocument(Rustfmt rustfmt, CargoProject cargoProject, Document document) {
        OpenApiUtil.checkIsDispatchThread();
        if (!document.isWritable()) return;
        CharSequence formattedText = rustfmt.reformatDocumentTextOrNull(cargoProject, document);
        if (formattedText == null) return;
        consulo.application.WriteAction.run(() -> document.setText(formattedText));
    }

    boolean isSuppressed() {
        return isSuppressed;
    }

    /**
     * Saves all documents "as they are" (without trailing spaces stripping).
     * <p>
     * Upstream additionally queued each document for later stripping by reflecting into
     * {@code FileDocumentManagerImpl.myTrailingSpacesStripper}; both classes are platform-internal
     * and the field poking would not survive JPMS anyway, so that part is dropped.
     */
    public static void saveAllDocumentsAsTheyAre(boolean reformatLater) {
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        RustfmtWatcher rustfmtWatcher = getInstance();
        rustfmtWatcher.withoutReformatting(() -> {
            for (Document document : documentManager.getUnsavedDocuments()) {
                documentManager.saveDocumentAsIs(document);
                if (reformatLater) rustfmtWatcher.reformatDocumentLater(document);
            }
        });
    }

    public static void saveAllDocumentsAsTheyAre() {
        saveAllDocumentsAsTheyAre(true);
    }

}
