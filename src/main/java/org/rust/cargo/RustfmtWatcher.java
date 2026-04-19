/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.DocumentUtil;
import com.intellij.util.containers.ContainerUtil;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Rustfmt;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;

@Service
public final class RustfmtWatcher {
    private final Set<Document> documentsToReformatLater = ContainerUtil.newConcurrentSet();
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

    private static RustfmtWatcher getInstanceIfCreated() {
        return ApplicationManager.getApplication().getService(RustfmtWatcher.class);
    }

    private static CargoProject findCargoProject(Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) return null;
        Project project = ProjectLocator.getInstance().guessProjectForFile(file);
        if (project == null) return null;
        return CargoProjectServiceUtil.getCargoProjects(project).findProjectForFile(file);
    }

    private static void reformatDocuments(CargoProject cargoProject, List<Document> documents) {
        Project project = cargoProject.getProject();
        if (!RsProjectSettingsServiceUtil.getRustfmtSettings(project).getRunRustfmtOnSave()) return;
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getRustSettings(project).getToolchain();
        if (toolchain == null) return;
        Rustfmt rustfmt = Rustfmt.create(toolchain);
        if (rustfmt == null) return;
        if (Rustup.checkNeedInstallRustfmt(cargoProject.getProject(), CargoCommandConfiguration.getWorkingDirectory(cargoProject))) return;
        for (Document document : documents) {
            reformatDocument(rustfmt, cargoProject, document);
        }
    }

    private static void reformatDocument(Rustfmt rustfmt, CargoProject cargoProject, Document document) {
        OpenApiUtil.checkIsDispatchThread();
        if (!document.isWritable()) return;
        CharSequence formattedText = rustfmt.reformatDocumentTextOrNull(cargoProject, document);
        if (formattedText == null) return;
        DocumentUtil.writeInRunUndoTransparentAction(() -> document.setText(formattedText));
    }

    public static class RustfmtListener implements FileDocumentManagerListener {

        @Override
        public void beforeAllDocumentsSaving() {
            RustfmtWatcher watcher = getInstanceIfCreated();
            if (watcher == null) return;
            Set<Document> documentsToReformatLater = watcher.documentsToReformatLater;
            List<Document> documentsToReformat = new ArrayList<>(documentsToReformatLater);
            documentsToReformatLater.clear();

            Map<CargoProject, List<Document>> grouped = new HashMap<>();
            for (Document document : documentsToReformat) {
                CargoProject cargoProject = findCargoProject(document);
                grouped.computeIfAbsent(cargoProject, k -> new ArrayList<>()).add(document);
            }

            for (Map.Entry<CargoProject, List<Document>> entry : grouped.entrySet()) {
                CargoProject cargoProject = entry.getKey();
                if (cargoProject == null) continue;

                if (DumbService.isDumb(cargoProject.getProject())) {
                    documentsToReformatLater.addAll(entry.getValue());
                } else {
                    reformatDocuments(cargoProject, entry.getValue());
                }
            }
        }

        @Override
        public void beforeDocumentSaving(Document document) {
            RustfmtWatcher watcher = getInstanceIfCreated();
            boolean suppressed = watcher != null && watcher.isSuppressed;
            if (!suppressed) {
                CargoProject cargoProject = findCargoProject(document);
                if (cargoProject == null) return;
                if (DumbService.isDumb(cargoProject.getProject())) {
                    getInstance().reformatDocumentLater(document);
                } else {
                    reformatDocuments(cargoProject, List.of(document));
                }
            }
        }

        @Override
        public void unsavedDocumentsDropped() {
            RustfmtWatcher watcher = getInstanceIfCreated();
            if (watcher != null) {
                watcher.documentsToReformatLater.clear();
            }
        }
    }
}
