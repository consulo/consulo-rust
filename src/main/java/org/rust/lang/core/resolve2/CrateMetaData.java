/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.CargoWorkspaceData;

public class CrateMetaData {
    @NotNull
    private final CargoWorkspace.Edition edition;
    @NotNull
    private final String name;
    @Nullable
    private final CargoWorkspaceData.ProcMacroArtifact procMacroArtifact;

    public CrateMetaData(
        @NotNull CargoWorkspace.Edition edition,
        @NotNull String name,
        @Nullable CargoWorkspaceData.ProcMacroArtifact procMacroArtifact
    ) {
        this.edition = edition;
        this.name = name;
        this.procMacroArtifact = procMacroArtifact;
    }

    @NotNull
    public CargoWorkspace.Edition getEdition() {
        return edition;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @Nullable
    public CargoWorkspaceData.ProcMacroArtifact getProcMacroArtifact() {
        return procMacroArtifact;
    }
}
