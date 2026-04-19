/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Keeps timestamps of previous cargo metadata invocation for each cargo project
 * to check changes in Cargo.lock should be skipped and avoid unnecessary project loading.
 */
@Service
public final class CargoEventService {

    private final ConcurrentMap<Path, Long> metadataCallTimestamps = new ConcurrentHashMap<>();

    public CargoEventService(@NotNull Project project) {
        project.getMessageBus().connect().subscribe(
            CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (service, projects) -> {
                Set<Path> projectDirs = projects.stream()
                    .map(CargoCommandConfiguration::getWorkingDirectory)
                    .collect(Collectors.toSet());
                metadataCallTimestamps.keySet().retainAll(projectDirs);
            }
        );
    }

    public void onMetadataCall(@NotNull Path projectDirectory) {
        metadataCallTimestamps.put(projectDirectory, System.currentTimeMillis());
    }

    @Nullable
    public Long extractTimestamp(@NotNull Path projectDirectory) {
        return metadataCallTimestamps.remove(projectDirectory);
    }

    @NotNull
    public static CargoEventService getInstance(@NotNull Project project) {
        return project.getService(CargoEventService.class);
    }
}
