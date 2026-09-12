/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.document.Document;
import consulo.document.event.FileDocumentManagerListener;
import consulo.project.DumbService;
import org.rust.cargo.project.model.CargoProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runs rustfmt over documents as they are saved, deferring the work while the project is indexing.
 */
@TopicImpl(ComponentScope.APPLICATION)
public class RustfmtOnSaveListener implements FileDocumentManagerListener {

    @Override
    public void beforeAllDocumentsSaving() {
        RustfmtWatcher watcher = RustfmtWatcher.getInstanceIfCreated();
        if (watcher == null) return;
        Set<Document> documentsToReformatLater = watcher.documentsToReformatLater;
        List<Document> documentsToReformat = new ArrayList<>(documentsToReformatLater);
        documentsToReformatLater.clear();

        Map<CargoProject, List<Document>> grouped = new HashMap<>();
        for (Document document : documentsToReformat) {
            CargoProject cargoProject = RustfmtWatcher.findCargoProject(document);
            grouped.computeIfAbsent(cargoProject, k -> new ArrayList<>()).add(document);
        }

        for (Map.Entry<CargoProject, List<Document>> entry : grouped.entrySet()) {
            CargoProject cargoProject = entry.getKey();
            if (cargoProject == null) continue;

            if (DumbService.isDumb(cargoProject.getProject())) {
                documentsToReformatLater.addAll(entry.getValue());
            } else {
                RustfmtWatcher.reformatDocuments(cargoProject, entry.getValue());
            }
        }
    }

    @Override
    public void beforeDocumentSaving(Document document) {
        RustfmtWatcher watcher = RustfmtWatcher.getInstanceIfCreated();
        if (watcher != null && watcher.isSuppressed()) return;

        CargoProject cargoProject = RustfmtWatcher.findCargoProject(document);
        if (cargoProject == null) return;
        if (DumbService.isDumb(cargoProject.getProject())) {
            RustfmtWatcher.getInstance().reformatDocumentLater(document);
        } else {
            RustfmtWatcher.reformatDocuments(cargoProject, List.of(document));
        }
    }

    @Override
    public void unsavedDocumentsDropped() {
        RustfmtWatcher watcher = RustfmtWatcher.getInstanceIfCreated();
        if (watcher != null) {
            watcher.documentsToReformatLater.clear();
        }
    }
}
