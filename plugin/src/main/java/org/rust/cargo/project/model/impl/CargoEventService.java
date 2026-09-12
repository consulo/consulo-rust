/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;
import jakarta.inject.Inject;

/**
 * Keeps timestamps of previous cargo metadata invocation for each cargo project
 * to check changes in Cargo.lock should be skipped and avoid unnecessary project loading.
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class CargoEventService {

    private final ConcurrentMap<Path, Long> metadataCallTimestamps = new ConcurrentHashMap<>();

    @Inject

    public CargoEventService(@Nonnull Project project) {
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

    public void onMetadataCall(@Nonnull Path projectDirectory) {
        metadataCallTimestamps.put(projectDirectory, System.currentTimeMillis());
    }

    @Nullable
    public Long extractTimestamp(@Nonnull Path projectDirectory) {
        return metadataCallTimestamps.remove(projectDirectory);
    }

    @Nonnull
    public static CargoEventService getInstance(@Nonnull Project project) {
        return project.getService(CargoEventService.class);
    }
}
