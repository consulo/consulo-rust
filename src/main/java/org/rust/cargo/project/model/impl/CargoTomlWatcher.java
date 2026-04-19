/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.RsConstants;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * File changes listener, detecting changes inside the Cargo.toml files
 * and creation of *.rs files acting as automatic crate root.
 */
public class CargoTomlWatcher implements BulkFileListener {

    private static final Logger LOG = Logger.getInstance(CargoTomlWatcher.class);

    private static final List<String> IMPLICIT_TARGET_FILES = List.of(
        "/build.rs", "/src/main.rs", "/src/lib.rs"
    );

    private static final List<String> IMPLICIT_TARGET_DIRS = List.of(
        "/src/bin", "/examples", "/tests", "/benches"
    );

    private final CargoProjectsService cargoProjects;
    private final Runnable onCargoTomlChange;

    public CargoTomlWatcher(@NotNull CargoProjectsService cargoProjects, @NotNull Runnable onCargoTomlChange) {
        this.cargoProjects = cargoProjects;
        this.onCargoTomlChange = onCargoTomlChange;
    }

    @Override
    public void before(@NotNull List<? extends VFileEvent> events) {
        // no-op
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        if (events.stream().anyMatch(this::isInterestingEvent)) {
            onCargoTomlChange.run();
        }
    }

    private boolean isInterestingEvent(@NotNull VFileEvent event) {
        if (!isInterestingEventStatic(cargoProjects.getProject(), event)) return false;

        if (event instanceof VFileContentChangeEvent contentChange) {
            VirtualFile file = contentChange.getFile();
            Path fileParentPath = OpenApiUtil.getPathAsPath(file).getParent();
            var pkg = cargoProjects.findPackageForFile(file);
            if (pkg != null && pkg.getOrigin() == PackageOrigin.WORKSPACE) return true;
            for (var cp : cargoProjects.getAllProjects()) {
                if (cp.getManifest().getParent().equals(fileParentPath)) return true;
            }
            return false;
        }
        return true;
    }

    @VisibleForTesting
    public static boolean isInterestingEventStatic(@NotNull Project project, @NotNull VFileEvent event) {
        if (pathEndsWith(event, CargoConstants.MANIFEST_FILE)) return true;

        if (pathEndsWith(event, CargoConstants.LOCK_FILE)) {
            Path projectDir = Paths.get(event.getPath()).getParent();
            Long timestamp = CargoEventService.getInstance(project).extractTimestamp(projectDir);
            long ts = timestamp != null ? timestamp : 0;
            if (event.getRequestor() != null) return true;
            long current = System.currentTimeMillis();
            int delayThreshold = Registry.intValue("org.rust.cargo.lock.update.delay.threshold");
            long delay = current - ts;
            if (delay > delayThreshold) {
                LOG.info("External change in " + event.getPath() + ". Previous Cargo metadata call was " + delay + " ms before");
                return true;
            } else {
                LOG.info("Skip external change for " + event.getPath() + ". Previous Cargo metadata call was " + delay + " ms before");
                return false;
            }
        }

        if (event instanceof VFileContentChangeEvent) return false;
        if (!pathEndsWith(event, ".rs")) return false;
        if (event instanceof VFilePropertyChangeEvent propEvent) {
            if (!VirtualFile.PROP_NAME.equals(propEvent.getPropertyName())) return false;
        }

        if (IMPLICIT_TARGET_FILES.stream().anyMatch(suffix -> pathEndsWith(event, suffix))) return true;

        String parent = PathUtil.getParentPath(event.getPath());
        String grandParent = PathUtil.getParentPath(parent);
        return IMPLICIT_TARGET_DIRS.stream().anyMatch(dir ->
            parent.endsWith(dir) || (pathEndsWith(event, RsConstants.MAIN_RS_FILE) && grandParent.endsWith(dir))
        );
    }

    private static boolean pathEndsWith(@NotNull VFileEvent event, @NotNull String suffix) {
        if (event.getPath().endsWith(suffix)) return true;
        if (event instanceof VFilePropertyChangeEvent propEvent) {
            return propEvent.getOldPath().endsWith(suffix);
        }
        return false;
    }
}
