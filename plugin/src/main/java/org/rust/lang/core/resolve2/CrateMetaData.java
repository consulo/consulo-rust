/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.CargoWorkspaceData;

public class CrateMetaData {
    @Nonnull
    private final CargoWorkspace.Edition edition;
    @Nonnull
    private final String name;
    @Nullable
    private final CargoWorkspaceData.ProcMacroArtifact procMacroArtifact;

    public CrateMetaData(
        @Nonnull CargoWorkspace.Edition edition,
        @Nonnull String name,
        @Nullable CargoWorkspaceData.ProcMacroArtifact procMacroArtifact
    ) {
        this.edition = edition;
        this.name = name;
        this.procMacroArtifact = procMacroArtifact;
    }

    @Nonnull
    public CargoWorkspace.Edition getEdition() {
        return edition;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nullable
    public CargoWorkspaceData.ProcMacroArtifact getProcMacroArtifact() {
        return procMacroArtifact;
    }
}
