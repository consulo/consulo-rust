/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsListener;
import org.rust.cargo.project.model.CargoProjectsService;

import java.util.Collection;

/**
 * Bumps the Rust structure modification count when the cargo projects change.
 * <p>
 * Everything derived from the workspace is cached against that count - above all the crate a file
 * belongs to. A file opened before the first sync finishes answers that it belongs to no crate, and
 * without this the answer would stay cached and every reference in it would stay unresolved.
 * {@link RsPsiManagerImpl} subscribes as well, but only once something has asked for it; this runs
 * whether or not the service has been created.
 */
@TopicImpl(ComponentScope.PROJECT)
public class RsCargoStructureInvalidator implements CargoProjectsListener {

    @Nullable
    private final Project project;

    public RsCargoStructureInvalidator() {
        this(null);
    }

    public RsCargoStructureInvalidator(@Nullable Project project) {
        this.project = project;
    }

    @Override
    public void cargoProjectsUpdated(@Nonnull CargoProjectsService service, @Nonnull Collection<CargoProject> projects) {
        Project target = project != null ? project : service.getProject();
        if (target.isDisposed()) return;
        target.getInstance(RsPsiManager.class).incRustStructureModificationCount();
    }
}
