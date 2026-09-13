/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import jakarta.annotation.Nonnull;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.workspace.CargoWorkspace;

import java.util.Collection;

/**
 * A value in the Cargo tree. Every node carries the Cargo project it belongs to, so a selection
 * anywhere in the tree can be resolved back to its project without walking the parent chain.
 */
public sealed interface CargoTreeNode {

    @Nonnull
    CargoProject cargoProject();

    record ProjectNode(@Nonnull CargoProject cargoProject) implements CargoTreeNode {
    }

    record MemberNode(@Nonnull CargoProject cargoProject, @Nonnull CargoWorkspace.Package pkg) implements CargoTreeNode {
    }

    record TargetsNode(@Nonnull CargoProject cargoProject,
                       @Nonnull Collection<CargoWorkspace.Target> targets) implements CargoTreeNode {
    }

    record TargetNode(@Nonnull CargoProject cargoProject, @Nonnull CargoWorkspace.Target target) implements CargoTreeNode {
    }
}
